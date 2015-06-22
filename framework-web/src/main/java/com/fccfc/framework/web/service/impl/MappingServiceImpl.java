/**
 * 
 */
package com.fccfc.framework.web.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.fccfc.framework.api.ServiceException;
import com.fccfc.framework.core.config.Configuration;
import com.fccfc.framework.core.db.DaoException;
import com.fccfc.framework.core.utils.CommonUtil;
import com.fccfc.framework.web.dao.UrlResourceDao;
import com.fccfc.framework.web.service.MappingService;

/**
 * <Description> <br>
 * 
 * @author 王伟<br>
 * @version 1.0<br>
 * @taskId <br>
 * @CreateDate 2014年11月17日 <br>
 * @since V1.0<br>
 * @see com.fccfc.framework.web.service.impl <br>
 */
@Service
public class MappingServiceImpl implements MappingService {

    private List<String> resourceCache = new ArrayList<String>();

    @Resource
    private UrlResourceDao urlResourceDao;

    /*
     * (non-Javadoc)
     * @see com.fccfc.framework.web.service.MappingService#selectAllUrlResource()
     */
    @Override
    public List<String> selectAllUrlResource() throws ServiceException {

        if (CommonUtil.isEmpty(resourceCache)) {
            try {
                List<String> moduleCodes = Configuration.getModuleCode();
                if (CommonUtil.isNotEmpty(moduleCodes)) {
                    resourceCache = urlResourceDao.selectAllModuleUrlResource(moduleCodes);
                }
            }
            catch (DaoException e) {
                throw new ServiceException(e);
            }
        }
        return resourceCache;
    }

    /*
     * (non-Javadoc)
     * @see com.fccfc.framework.web.service.MappingService#getMethodUrl(int, java.lang.String)
     */
    @Override
    public String getMethodUrl(String clazzName, String methodName) throws ServiceException {
        try {
            return urlResourceDao.selectUrlByClassAndName(Configuration.getModuleCode(), clazzName, methodName);
        }
        catch (DaoException e) {
            throw new ServiceException(e);
        }
    }

}