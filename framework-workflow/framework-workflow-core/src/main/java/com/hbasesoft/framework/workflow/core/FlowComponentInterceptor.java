/**************************************************************************************** 
 Copyright © 2003-2012 hbasesoft Corporation. All rights reserved. Reproduction or       <br>
 transmission in whole or in part, in any form or by any means, electronic, mechanical <br>
 or otherwise, is prohibited without the prior written consent of the copyright owner. <br>
 ****************************************************************************************/
package com.hbasesoft.framework.workflow.core;

import java.util.Map;

import com.hbasesoft.framework.workflow.core.config.FlowConfig;

/**
 * <Description> <br>
 * 
 * @author 王伟<br>
 * @version 1.0<br>
 * @taskId <br>
 * @CreateDate 2017年9月3日 <br>
 * @since V1.0<br>
 * @see com.hbasesoft.framework.workflow.core <br>
 */
public interface FlowComponentInterceptor {

    default int order() {
        return 0;
    }

    boolean before(FlowBean flowBean, FlowConfig flowConfig, Map<String, Object> paramMap);

    void after(FlowBean flowBean, FlowConfig flowConfig, Map<String, Object> paramMap);

    void error(Exception e, FlowBean flowBean, FlowConfig flowConfig, Map<String, Object> paramMap);

}
