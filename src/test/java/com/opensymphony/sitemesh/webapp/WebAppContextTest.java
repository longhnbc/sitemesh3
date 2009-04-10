package com.opensymphony.sitemesh.webapp;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.decorator.map.PathBasedDecoratorSelector;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.html.rules.decorator.SiteMeshDecorateRule;
import com.opensymphony.sitemesh.tagprocessor.State;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;
import junit.framework.TestCase;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Joe Walnes
 */
public class WebAppContextTest extends TestCase {

    public void testDispatchesToServletToApplyDecorator() throws Exception {

        HttpServlet decoratorServlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                Content content = (Content) request.getAttribute(WebAppContext.CONTENT_KEY);
                PrintWriter out = response.getWriter();
                out.println("Title = " + content.getProperty("title"));
            }
        };

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new BaseSiteMeshFilter(
                        new BasicSelector("text/html"),
                        new HtmlContentProcessor<WebAppContext>(),
                        new PathBasedDecoratorSelector().put("/*", "/mydecorator")
                ))
                .addStaticContent("/mycontent", "text/html", "<title>Some title</title>")
                .addServlet("/mydecorator", decoratorServlet)
                .create();

        webEnvironment.doGet("/mycontent");
        assertEquals("Title = Some title", webEnvironment.getBody().trim());
    }

    public void testSupportsDecoratingInlineContent() throws Exception {

        ContentProcessor<WebAppContext> processor = new HtmlContentProcessor<WebAppContext>() {
            @Override
            protected void setupRules(State defaultState, Content content, WebAppContext context) {
                super.setupRules(defaultState, content, context);
                defaultState.addRule("sitemesh:decorate", new SiteMeshDecorateRule(context));
            }
        };

        WebEnvironment web = new WebEnvironment.Builder()
                .addFilter("/*", new BaseSiteMeshFilter(
                        new BasicSelector("text/html"),
                        processor,
                        new PathBasedDecoratorSelector()
                                .put("/*", "/decorators/page.html")
                ))
                .addStaticContent("/hello.html", "text/html", "" +
                        "<body>\n" +
                        "CONTENT\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block A'><b>A</b></sitemesh:decorate>\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block B'><i>B</i></sitemesh:decorate>\n" +
                        "</body>\n")
                .addStaticContent("/decorators/page.html", "text/html",
                        "PAGE\n<sitemesh:write property='body'/>\n/PAGE")
                .addStaticContent("/decorators/inline.html", "text/html", "" +
                        "INLINE Title:<sitemesh:write property='title'/> " +
                        "Body:<sitemesh:write property='body'/> /INLINE")
                .create();

        web.doGet("/hello.html");

        String expected = "" +
                "PAGE\n" +
                "\n" +
                "CONTENT\n" +
                "INLINE Title:block A Body:<b>A</b> /INLINE\n" +
                "INLINE Title:block B Body:<i>B</i> /INLINE\n" +
                "\n" +
                "/PAGE";

        assertEquals(expected, web.getBody());
    }

    public void testSupportsDecoratingInlineContentInDecorators() throws Exception {

        ContentProcessor<WebAppContext> processor = new HtmlContentProcessor<WebAppContext>() {
            @Override
            protected void setupRules(State defaultState, Content content, WebAppContext context) {
                super.setupRules(defaultState, content, context);
                defaultState.addRule("sitemesh:decorate", new SiteMeshDecorateRule(context));
            }
        };

        WebEnvironment web = new WebEnvironment.Builder()
                .addFilter("/*", new BaseSiteMeshFilter(
                        new BasicSelector("text/html"),
                        processor,
                        new PathBasedDecoratorSelector()
                                .put("/*", "/decorators/page.html")
                ))
                .addStaticContent("/hello.html", "text/html", "" +
                        "<body>\n" +
                        "CONTENT\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block A'><b>A</b></sitemesh:decorate>\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block B'><i>B</i></sitemesh:decorate>\n" +
                        "</body>\n")
                .addStaticContent("/decorators/page.html", "text/html", "" +
                        "PAGE\n<sitemesh:decorate decorator='/decorators/inner.html'>" +
                        "<sitemesh:write property='body'/></sitemesh:decorate>\n/PAGE")
                .addStaticContent("/decorators/inline.html", "text/html", "" +
                        "INLINE Title:<sitemesh:write property='title'/> " +
                        "Body:<sitemesh:decorate decorator='/decorators/inner.html'>" +
                        "<sitemesh:write property='body'/></sitemesh:decorate> /INLINE")
                .addStaticContent("/decorators/inner.html", "text/html", "" +
                        "INNER<sitemesh:write property='body'/>/INNER")
                .create();

        web.doGet("/hello.html");

        String expected = "" +
                "PAGE\n" +
                "INNER\n" +
                "CONTENT\n" +
                "INLINE Title:block A Body:INNER<b>A</b>/INNER /INLINE\n" +
                "INLINE Title:block B Body:INNER<i>B</i>/INNER /INLINE\n" +
                "/INNER\n" +
                "/PAGE";

        assertEquals(expected, web.getBody());
    }

}
