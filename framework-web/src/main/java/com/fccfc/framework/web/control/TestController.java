/**
 * 
 */
package com.fccfc.framework.web.control;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * <Description> <br>
 * 
 * @author 王伟<br>
 * @version 1.0<br>
 * @taskId <br>
 * @CreateDate 2014年11月17日 <br>
 * @since V1.0<br>
 * @see com.fccfc.framework.web.control <br>
 */
@Controller
public class TestController {
    int index = 0;

    public String execute() {
        return "test/index";
    }

    public ModelAndView test(String name) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("hello", name);
        return new ModelAndView("test/say", map);
    }

    @ResponseBody
    public String test2(String name) {
        System.out.println(name);
        return name;
    }
}