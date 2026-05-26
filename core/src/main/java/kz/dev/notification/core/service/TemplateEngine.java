package kz.dev.notification.core.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import kz.dev.notification.core.domain.NotificationTemplate;
import kz.dev.notification.core.domain.RenderedNotification;
import kz.dev.notification.core.exception.TemplateRenderException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class TemplateEngine {

    private final Configuration freemarker;

    public TemplateEngine() {
        freemarker = new Configuration(Configuration.VERSION_2_3_33);
        freemarker.setDefaultEncoding("UTF-8");
    }

    public RenderedNotification render(NotificationTemplate template, Map<String, String> data) {
        try {
            String subject = template.getSubject() != null
                    ? process(template.getSubject(), data)
                    : null;
            String body = process(template.getBody(), data);

            RenderedNotification rendered = new RenderedNotification();
            rendered.setChannel(template.getChannel());
            rendered.setSubject(subject);
            rendered.setBody(body);
            return rendered;
        } catch (IOException | TemplateException e) {
            throw new TemplateRenderException(template.getEventType(), template.getChannel(), e);
        }
    }

    private String process(String templateStr, Map<String, String> data) throws IOException, TemplateException {
        Template tpl = new Template(null, new StringReader(templateStr), freemarker);
        StringWriter out = new StringWriter();
        tpl.process(data, out);
        return out.toString();
    }
}
