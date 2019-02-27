package pl.hordyjewiczmichal;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;


@SuppressWarnings("WeakerAccess")
public class GtagInjectorFilter implements Filter
{
    private String script;


    @Override
    public void init(FilterConfig config)
    {
        String trackingId = config.getInitParameter("tracking-id");
        this.script = "    <!-- Global site tag (gtag.js) - Google Analytics -->\n" +
                "    <script async src=\"https://www.googletagmanager.com/gtag/js?id=" + trackingId + "\"></script>\n" +
                "    <script>\n" +
                "        window.dataLayer = window.dataLayer || [];\n" +
                "        function gtag(){dataLayer.push(arguments);}\n" +
                "        gtag('js', new Date());\n" +
                "\n" +
                "        gtag('config', '" + trackingId + "');\n" +
                "    </script>";
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        CustomResponseWrapper wrapper = new CustomResponseWrapper((HttpServletResponse) response);

        if (((HttpServletRequest) request).getHeader("Accept").contains("text/html"))
        {
            filterChain.doFilter(request, wrapper);
            if (wrapper.getContentType().contains("text/html"))
            {
                response.setCharacterEncoding("UTF-8"); // required for static html files

                String content = wrapper.getCaptureAsString();
                // make replace below
                String replacedContent = content.replaceFirst("<head([^>]*)>", "<head$1>\n" + getScript());
                //

                CharArrayWriter writer = new CharArrayWriter();
                writer.write(replacedContent);

                PrintWriter out = response.getWriter();
                response.setContentLength(getFixedContentLength(replacedContent, response.getCharacterEncoding())); // must read content length from byte array
                out.write(writer.toString());
                out.close();
            }
            else
            {
                filterChain.doFilter(request, response);
            }
        }
        else
        {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void destroy()
    {
        // nothing to clean up
    }

    private int getFixedContentLength(String content, String encoding) throws IOException
    {
        return content.getBytes(encoding).length;
    }

    private String getScript()
    {
        return script;
    }
}
