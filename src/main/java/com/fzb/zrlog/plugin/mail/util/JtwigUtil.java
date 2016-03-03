package com.fzb.zrlog.plugin.mail.util;

import com.fzb.common.util.IOUtil;
import com.lyncode.jtwig.JtwigModelMap;
import com.lyncode.jtwig.JtwigTemplate;
import com.lyncode.jtwig.exception.CompileException;
import com.lyncode.jtwig.exception.ParseException;
import com.lyncode.jtwig.exception.RenderException;

import java.io.InputStream;
import java.util.Map;

public class JtwigUtil {

    private JtwigUtil() {

    }

    public static String render(String templateStr, Map<String, Object> map) {
        JtwigTemplate jtwigTemplate = JtwigTemplate.fromString(templateStr);
        JtwigModelMap modelMap = new JtwigModelMap();
        try {
            modelMap.putAll(map);
            return jtwigTemplate.output(modelMap);
        } catch (ParseException | CompileException | RenderException e) {
            e.printStackTrace();
        }
        return "<html><body>Not Found</body></html>";
    }

    public static String render(InputStream inputStream, Map<String, Object> map) {
        JtwigTemplate jtwigTemplate = JtwigTemplate.fromString(IOUtil.getStringInputStream(inputStream));
        JtwigModelMap modelMap = new JtwigModelMap();
        try {
            modelMap.putAll(map);
            return jtwigTemplate.output(modelMap);
        } catch (ParseException | CompileException | RenderException e) {
            e.printStackTrace();
        }
        return "<html><body>Not Found</body></html>";
    }
}
