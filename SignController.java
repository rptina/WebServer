package com.yunsign.business.controller;
sdadas
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.yunsign.business.entity.ApprovalFlowEntity;
import com.yunsign.business.entity.ContractEntity;
import com.yunsign.business.service.ContractManageService;
import com.yunsign.business.service.DataManageService;
import com.yunsign.business.service.SignService;
import com.yunsign.common.CheckPkcs;
import com.yunsign.common.Constant;
import com.yunsign.util.*;
import com.github.pagehelper.PageInfo;
import org.apache.ibatis.session.RowBounds;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.context.ContextLoader;

/**
 * 合同签署
 * @author Administrator
 *
 */
@Controller
@RequestMapping("/sign")
public class SignController {
    @Autowired
    SignService signService;

    @Autowired
    DataManageService dataManageService;

    @Autowired
    ContractManageService contractManageService;
    
    private Logger log = Logger.getLogger(SignController.class);
    
    /***************************zhuyuhua begin* @throws Exception ***********************/
    @RequestMapping(value = "/contractPage")
    public ModelAndView goToLoginPage(){
    	ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("certSelectForSign");
        
		return modelAndView;
    }
    
    /**
 	 * 检查证书
 	 * 
 	 * @param request
 	 * @return
 	 * @throws Exception
 	 */
 	@ResponseBody
 	@RequestMapping(value = "/checkpkcs.do")
 	public String checkpkcs(HttpServletRequest request) {

 		String certContent = StringForCertUtil.nullToString(request.getParameter("certContent"));
 		String certThumbPrint = StringForCertUtil.nullToString(request.getParameter("certThumbPrint"));
 		String certSerialNumber = StringForCertUtil.nullToString(request.getParameter("certSerialNumber"));
 		System.out.println("certContent==="+certContent+",\n certThumbPrint==="+certThumbPrint+" \n certSerialNumber==="+certSerialNumber);
 		if ("".equals(certContent)) {
 			return "100";
 		}
 		if ("".equals(certThumbPrint)) {
 			return "101";
 		}
 		if ("".equals(certSerialNumber)) {
 			return "102";
 		}

 		String result = "";
 		try {
 			result = CheckPkcs.checkpkcs1(certContent, certThumbPrint, certSerialNumber);
 			log.info("检查证书checkpkcs.do返回值：=====" + result);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 		return result;
 	}
 	
 	/**
	 * 激活买卖盾
	 * 
	 * @param request
	 * @return
 	 * @throws ParseException 
	 */
	@ResponseBody
	@RequestMapping(value = "/checkCert.do")
	public String checkCert(HttpServletRequest request)  {
		try
		{
		Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
		
		String ucid = user.get("id").toString();
		String companyName = user.get("companyName").toString();
		String certContent = (String) request.getParameter("certContent");//证书原文
		String certSerialNumber = (String) request.getParameter("certSerialNumber");//证书序列号
		String certThumbPrint = (String) request.getParameter("certThumbPrint");//证书指纹信息
		String certSubject = (String) request.getParameter("certSubject");//证书主题
		String certBeforeSystemTime = (String) request.getParameter("certBeforeSystemTime");//证书有效期，开始时间
		String certAfterSystemTime = (String) request.getParameter("certAfterSystemTime");//证书有效期，截止时间
		String certIssuer = (String) request.getParameter("certIssuer");//证书颁发者
		
		//certSubject 包不包含公司名称
		String returnStr = "111";
		
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("userId", ucid);
		paramMap.put("certContent", certContent);
		paramMap.put("certSerialNumber", certSerialNumber);
		paramMap.put("certThumbPrint", certThumbPrint);
		paramMap.put("certSubject", certSubject);
		paramMap.put("certBeforeSystemTime", certBeforeSystemTime);
		paramMap.put("certAfterSystemTime", certAfterSystemTime);
		paramMap.put("certIssuer", certIssuer);
		
		paramMap.put("status", "4");
	  	Map<String, Object> cert = signService.queryCertByUserAndCertNum(paramMap);
	  	
		if(cert ==null){
			return "444"; //证书未绑定
		} 
	 
	   if( !certSubject.contains(companyName)){
			 returnStr = "222";//key中公司名称和用户所属公司不一致
			 return returnStr;
		} 
		
		
		  
		//证书有效时间过期
		Long nowTime =  System.currentTimeMillis();//系统当前时间
		String  expiring_date_str = certAfterSystemTime;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date expiring_date;
		
			expiring_date = sdf.parse(expiring_date_str);
		
		Long expiring_time = expiring_date.getTime();
	//	暂时注释 rptina
	  if(expiring_time < nowTime){
			return "333"; //过期
		} 
	  	
	  	
		return returnStr;}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	 
	}
    
    //批量发起签约
	   @RequestMapping(value = "/batchSignPage")
	   public ModelAndView goTobathSignPage(HttpServletRequest request,
				  HttpServletResponse response,
				  @RequestParam(name = "contract_name", required = false) String contract_name,
				  @RequestParam(name = "contract_code", required = false) String contract_code,
				  @RequestParam(name = "dateStart", required = false) String dateStart,
				  @RequestParam(name = "dateEnd", required = false) String dateEnd,
				  @RequestParam(name = "pageNum", required = false, defaultValue = "1") Integer pageNum)  {
	   	ModelAndView modelAndView = new ModelAndView();
	   	Map<String, Object> queryCondition = new HashMap<String, Object>();//存放参数
	   	Boolean  flag =false;
	    Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
	       modelAndView.setViewName("batchSign");
	       Map<String, Object> map = new HashMap<String, Object>();
      String role_id = user.get("role_id").toString();
      String accountId = user.get("id").toString();
      map.put("role_id", role_id);
      queryCondition.put("accountId", accountId);
      
      map.put("tableName", "legal_person");
      map.put("auth_type", "2");//法人
      List<Map<String, Object>>   legalPersonList= signService.queryContractAuthByRoleId(map);
      map.put("tableName", "organization_type");
      map.put("auth_type", "3");//组织
      List<Map<String, Object>>   organizationTypeList= signService.queryContractAuthByRoleId(map);
      map.put("tableName", "contract_type");
      map.put("auth_type", "4");//合同类型
      List<Map<String, Object>>  contractTypeList= signService.queryContractAuthByRoleId(map);
      map.put("tableName", "contract_label");
      map.put("auth_type", "5");//合同标签
      List<Map<String, Object>> contractLabelList = signService.queryContractAuthByRoleId(map);
      
      //判断是否存在四种数据权限
      Map<String, Object> resultMap = new HashMap<String, Object>();
      if (legalPersonList.size() == 0){
   	   resultMap.put("legalPersonFlag", "N");
   	   	flag =true;
		   }
      if(organizationTypeList.size() == 0){
   	   resultMap.put("organizationFlag", "N");
   	    flag =true;
      }
      
      if(contractTypeList.size() == 0){
   	   resultMap.put("contractTypeFlag", "N");
   	   flag =true;
      }
      
      if(contractLabelList.size() == 0){
   	   resultMap.put("contractLabelFlag", "N");
   	   flag =true;
      }
      //权限都有,查询 创建表数据
      if(flag == false){
    	  if(!StringUtil.isEmptyString(contract_name)){
    		  queryCondition.put("contract_name", contract_name);
    		  modelAndView.addObject("contract_name", contract_name);
    	  }
    	  
    	  if(!StringUtil.isEmptyString(contract_code)){
    		  queryCondition.put("contract_code", contract_code);
    		  modelAndView.addObject("contract_code", contract_code);
    	  }
    	  
    	  if(!StringUtil.isEmptyString(dateStart)){
    		  queryCondition.put("dateStart", dateStart);
    		  modelAndView.addObject("dateStart", dateStart); 
    	  }
    	  
    	  if(!StringUtil.isEmptyString(dateEnd)){
    		  queryCondition.put("dateEnd", dateEnd);
    		  modelAndView.addObject("dateEnd", dateEnd);
    	  }
    	  
          //查询角色list
	        List<Map<String, Object>> list = signService.querySignContractByPage(new RowBounds(pageNum,Constant.PAGINATION_PAGE_MAX_SIZE),queryCondition);//Constant.PAGINATION_PAGE_MAX_SIZE
	        // 查询出的集合放进PageInfo分页控件    
	        PageInfo<Map<String, Object>> data = new PageInfo<Map<String, Object>>(list);
	        String url = request.getRequestURI() 
	        + "?contract_code=" + (contract_code == null ? "" : contract_code)
	        + "&contract_name=" + (contract_name == null ? "" : contract_name)
	        + "&dateStart=" + (dateStart == null ? "" : dateStart)
	        + "&dateEnd=" + (dateEnd == null ? "" : dateEnd);
	        
	        modelAndView.addObject("data", data);
	        modelAndView.addObject("url", url);
	        
	       List<Map<String, Object>>  attachList  = signService.findContracAttachByType("2",accountId);
	        
	       modelAndView.addObject("attachList", attachList);
       }
      	   modelAndView.addObject("authFlag", resultMap);
		   return modelAndView;
	   }
	   
	   
	   /**
	     * 批量新增合同规则
	     * @param file
	     * @return  
	 * @throws Exception 
	     */
	    @SuppressWarnings("unused")
		@RequestMapping(value = "/batchSignContract")
	    public void  batchSignContract(HttpServletRequest request,HttpServletResponse response,
	            @RequestParam(value = "file", required = false) MultipartFile file)   {
	    	 try
				{
	        log.info("In /batchAddUser");
	        log.info("批量添加开始"); 
	        log.info("fileName: " + file.getOriginalFilename());
	        Map<String,Object> resultMsg = new HashMap<String,Object>();
	        List<String> jzErr = new ArrayList<String>();//匹配不到矩阵
	        Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
		    String accountId = user.get("id").toString();
		    String role_id = user.get("role_id").toString();
		    
		    List<String> ec_contract_id_list=new ArrayList<String>();
		   
	        if(file == null) {
	        	resultMsg.put("isError", "Y");
	        	resultMsg.put("errMsg","上传文件不存在");
	        } else{
            log.info("FileName : " + file.getOriginalFilename());
           
				resultMsg = ExcelReaderUtil.readExcelForSign(file.getInputStream(), file.getOriginalFilename());
			
	        }  
	        if("N".equals(resultMsg.get("isError"))){
	        	//入表前判断审批矩阵 如果没有对应的矩阵 弹出错误信息
	        	List<Map<String,Object>> resultList = (List<Map<String, Object>>) resultMsg.get("excelResut");
	        	//匹配不到审批矩阵 isError 为M
	        	int i = 0;
	        	Boolean b  = true;
	        	for (Map<String, Object> mappObj : resultList){
	        		i++;
	        		mappObj.put("accountId", accountId);
	        		String legalPerson = mappObj.get("legal_person").toString();//法人
	        		String organization = mappObj.get("organization").toString();//组织
	        		String contractType = mappObj.get("contract_type").toString();//合同类型
	        		String contractLable = mappObj.get("contract_lable").toString();//合同标签
	        		String jdeCode = mappObj.get("supplier_jde_code").toString();
	        		String contract_code = mappObj.get("contract_code").toString();//合同编号
	        		String supplier_name = mappObj.get("supplier_name").toString();//合同编号
	        		
	        		//先判断是否存在ALL权限  这些数据权限 有查出code 拼个联合合同id
	        		Map<String, Object> dataAuthMap = new HashMap<String, Object>();
	        		dataAuthMap.put("auth_name", "签约法人ALL");
	        		dataAuthMap.put("tableName", "legal_person");  
	        		dataAuthMap.put("role_id",role_id);
	        		dataAuthMap.put("auth_type", "2");//法人 
	        		Map<String, Object> legalPersonCode  = signService.queryDateAuthCode(dataAuthMap);
	        		
	        		dataAuthMap.put("auth_name", "组织类别ALL"); 
	        		dataAuthMap.put("tableName", "organization_type");
	        		dataAuthMap.put("role_id",role_id);
	        		dataAuthMap.put("auth_type", "3");//组织 
	        		Map<String, Object> organizationCode  = signService.queryDateAuthCode(dataAuthMap);
	        		
	        		dataAuthMap.put("auth_name", "合同类型ALL");
	        		dataAuthMap.put("tableName", "contract_type");
	        		dataAuthMap.put("role_id",role_id);
	        		dataAuthMap.put("auth_type", "4");//合同类型
	        		Map<String, Object> contractTypeCode  = signService.queryDateAuthCode(dataAuthMap);
	        		
	        		dataAuthMap.put("auth_name", "合同标签ALL");
	        		dataAuthMap.put("tableName", "contract_label");
	        		dataAuthMap.put("role_id",role_id);
	        		dataAuthMap.put("auth_type", "5");//合同标签
	        		Map<String, Object> contractLableCode  = signService.queryDateAuthCode(dataAuthMap);
	        		
	        		//法人
	        		if(legalPersonCode == null){//没有法人ALL权限  
	        			//先判断是否存在  这些数据权限 有查出code 拼个联合合同id
		        		dataAuthMap.put("auth_name", legalPerson);
		        		dataAuthMap.put("tableName", "legal_person");  
		        		dataAuthMap.put("role_id",role_id);
		        		dataAuthMap.put("auth_type", "2");//法人 
		        		legalPersonCode  = signService.queryDateAuthCode(dataAuthMap);
	        		}else{
	        			dataAuthMap.put("auth_name", legalPerson);
		        		dataAuthMap.put("tableName", "legal_person");  
		        		legalPersonCode  = signService.queryAllDateAuthCode(dataAuthMap);
	        		}
	        		
	        		//组织
	        		if(organizationCode == null){//没有法人ALL权限
	        			//先判断是否存在  这些数据权限 有查出code 拼个联合合同id
	        			dataAuthMap.put("auth_name", organization); 
		        		dataAuthMap.put("tableName", "organization_type");
		        		dataAuthMap.put("role_id",role_id);
		        		dataAuthMap.put("auth_type", "3");//组织 
		        		 organizationCode  = signService.queryDateAuthCode(dataAuthMap);
	        		}else{
	        			dataAuthMap.put("auth_name", organization);
		        		dataAuthMap.put("tableName", "organization_type");  
		        		organizationCode  = signService.queryAllDateAuthCode(dataAuthMap);
	        		}
	        		
	        		//合同类型
	        		if(contractTypeCode == null){//没有法人ALL权限
	        			dataAuthMap.put("auth_name", contractType);
		        		dataAuthMap.put("tableName", "contract_type");
		        		dataAuthMap.put("role_id",role_id);
		        		dataAuthMap.put("auth_type", "4");//合同类型
		        		contractTypeCode  = signService.queryDateAuthCode(dataAuthMap);
	        		}else{
	        			dataAuthMap.put("auth_name", contractType);
		        		dataAuthMap.put("tableName", "contract_type");  
		        		contractTypeCode  = signService.queryAllDateAuthCode(dataAuthMap);
	        		}
	        		
	        		//合同标签
	        		if(contractLableCode == null){//没有法人ALL权限
	        			dataAuthMap.put("auth_name", contractLable);
		        		dataAuthMap.put("tableName", "contract_label");
		        		dataAuthMap.put("role_id",role_id);
		        		dataAuthMap.put("auth_type", "5");//合同标签
		        		contractLableCode  = signService.queryDateAuthCode(dataAuthMap);
	        		}else{
	        			dataAuthMap.put("auth_name", contractLable);
		        		dataAuthMap.put("tableName", "contract_label");  
		        		contractLableCode  = signService.queryAllDateAuthCode(dataAuthMap);
	        		}
	        		
	        		//判断jde是否存在
	        		Map<String, Object> jdeObj  = signService.queryJdeCode(jdeCode);
	        		
	        		Boolean isAuthExist = true;
	        		if(legalPersonCode ==null){
	        			resultMsg.put("isError", "M");
	        			String msg ="第"+i+"行数据签约法人不存在";
	        			jzErr.add(msg);
	        			isAuthExist = false;
	        		}  
	        		
	        		if(organizationCode ==null){
	        			resultMsg.put("isError", "M");
	        			String msg ="第"+i+"行数据组织类别不存在";
	        			jzErr.add(msg);
	        			isAuthExist = false;
	        		} 
	        		
	        		if(contractTypeCode ==null){
	        			resultMsg.put("isError", "M");
	        			String msg ="第"+i+"行数据合同类型不存在";
	        			jzErr.add(msg);
	        			isAuthExist = false;
	        		} 
	        		
	        		if(contractLableCode ==null){
	        			resultMsg.put("isError", "M");
	        			String msg ="第"+i+"行数据合同分类标签不存在";
	        			jzErr.add(msg);
	        			isAuthExist = false;
	        		}
	        		
	        		//jde是否存在
	        		if(jdeObj ==null){ 
	        			resultMsg.put("isError", "M");
	        			String msg ="第"+i+"行jdeCode不存在";
	        			jzErr.add(msg);
	        			isAuthExist = false;
	        		}else{//供应商名称是否一致
	           			String supplier_name_biaozhun = jdeObj.get("supplier_name").toString();
	           			if(!supplier_name_biaozhun.equals(supplier_name)){
	           				resultMsg.put("isError", "M");
	           	   			String msg ="第"+i+"行供应商名称与jdeCode不一致";
	           	   			jzErr.add(msg);
	           	   			isAuthExist = false;
	           			}
	           		}
	        		
	        		if(isAuthExist == false){
	        			b = false;
	        			continue;
	        		}else{
	        			String ec_contract_id = legalPersonCode.get("code").toString()
	        									+organizationCode.get("code").toString()
	        									+contractTypeCode.get("code").toString()
	        									+jdeObj.get("jde_code").toString()
	        									+contract_code;
	        			
	        			if(ec_contract_id_list.contains(ec_contract_id)){
	        				resultMsg.put("isError", "M");
	        				String msg ="第"+i+"行已存在相同数据权限的合同";
	        				jzErr.add(msg); 
	        				b = false;
	        				continue; 
	        			}else{
	        				ec_contract_id_list.add(ec_contract_id);
	        			}
	        			
	        			//判断联合合同id库中是否存在 ec_contract_id校验 返回错误信息  TODO  已生成的合同库内
	        		/* 	List<Map<String, Object>> contracts = signService.queryContractsbyEcContractId(ec_contract_id);
	        			if(contracts.size() !=0){
	        				resultMsg.put("isError", "M");
	        				String msg ="第"+i+"行已存在相同数据权限的合同";
	        				jzErr.add(msg); 
	        				b = false;
	        				continue; 
	        			}  */
	        			mappObj.put("supplier_account_id", jdeObj.get("account_id").toString());
	        			mappObj.put("ec_contract_id", ec_contract_id);
	        			mappObj.put("legal_person_id",legalPersonCode.get("id").toString()); 
	        			mappObj.put("organization_id",organizationCode.get("id").toString());
	        			mappObj.put("contract_type_id",contractTypeCode.get("id").toString());
	        			mappObj.put("contract_lable_id",contractLableCode.get("id").toString());
	        		}
	        		
	        		int approvalId =	dataManageService.getApprovalBy4Attr(legalPerson, organization, contractType, contractLable);
	        		if(approvalId==0){
	        			resultMsg.put("isError", "M");
	        			String msg ="第"+i+"行数据匹配不到审批矩阵";
	        			jzErr.add(msg);
	        			b = false;
	        			continue;
	        		} else{
	        			mappObj.put("approval_id", approvalId);
	        		}
				}
	        	resultMsg.put("jzErr", jzErr);
	        	//每条都能匹配到审批矩阵
	        	if(b){
	        		//合同联合编号库中重复 错误信息返回 TODO
	        		signService.createContractInfo(resultList,accountId); 
	        	}
	        	
	        }  
	        String jsonStr = JSONObject.toJSONString(resultMsg);  
	        
	        response.setContentType("text/html;charset=UTF-8");
	        response.getWriter().print(jsonStr);}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    }
	
	    
	  @RequestMapping(value = "/queryAttachMent")
	  public @ResponseBody List<Map<String,Object>> queryAttachMent(HttpServletRequest request,
				  HttpServletResponse response)
	    { 
		  Map<String,Object> paramMap = new HashMap<String,Object>();
		  Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
		  String accountId = user.get("id").toString();
		  paramMap.put("accountId", accountId);
		  return signService.queryAllAttach(paramMap);	 
	    }
	  
	 @RequestMapping("uploadAttach")
	 @ResponseBody
	public String uploads(@RequestParam("file")MultipartFile  file, String destDir,
	        HttpServletRequest request,HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=UTF-8");
	     Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
    String account = (String)user.get("account");
    String id =  user.get("id").toString();
    String fileName = file.getOriginalFilename();
    Map<String, Object> resultMap = new HashMap<String, Object>();
    //通过文件名查询附件表，不存在直接返回
    List<Map<String, Object>> contracAttachMentList =  signService.findContracAttachByName(fileName,id);
    if(contracAttachMentList.size()==0){
    	resultMap.put("flag","N" );
    	resultMap.put("msg", "附件名字不一致");
    	String jsonStr = JSONObject.toJSONString(resultMap);
    	response.getWriter().print(jsonStr);
   	 	return null; 
    }

    //将用户上传的pdf文档，上传到配置文件common.properties文件指定的路径
    String destPath = PropertiesUtil.getProperties().readValue("system.attach.filepath");
    //查询用户表 attachPiciNum 与account拼接文件夹名  提交审核成功后更新attachPiciNum +1为下一批次合同信息创建做准备
    Map<String, Object> dirInfo =  signService.queryUserAttachPiciNum(id);
    destPath +=account+dirInfo.get("attachPiciNum");

    String filePath =destPath+"/"+fileName;
    
    if (file.getSize() > Constant.FILE_SIZE)
    {
    	resultMap.put("flag","N" );
    	resultMap.put("msg", "您上传的文件大小已经超出范围");
    	String jsonStr = JSONObject.toJSONString(resultMap);
    	response.getWriter().print(jsonStr);
    	return null;
    }
    
    File destFile = new File(destPath);
    if (!destFile.exists())
    {
        destFile.mkdirs();
    }
    File f = new File(filePath);
    try
    {
        file.transferTo(f);
        f.createNewFile();
    }
    catch (IllegalStateException e)
    {
        e.printStackTrace();
    }
    catch (IOException e)
    {
        e.printStackTrace();
        resultMap.put("flag","N" );
    	resultMap.put("msg", "文件上传失败");
    	String jsonStr = JSONObject.toJSONString(resultMap);
    	response.getWriter().print(jsonStr);
    	return null;
    }
    //文件上传成功  更新文件状态
    //更新表
    signService.updateAttacHStatus("1",fileName,filePath);//1已上传
    
    resultMap.put("flag","Y" );
	resultMap.put("msg", "上传成功");
	String jsonStr = JSONObject.toJSONString(resultMap);
	response.getWriter().print(jsonStr);
	
	    return null;
    
}
	 
	//删除合同
	  @RequestMapping(value = "/deleteht", method = RequestMethod.GET)
	  public @ResponseBody Map<String,Object> deleteht(HttpServletRequest request, HttpServletResponse response)
	    {
		  	//得参
		  	String paramsStr =  request.getParameter("params") ;
		  	JSONObject jsonObject = JSONArray.parseObject(paramsStr);
		  	Map<String, Object> queryCondition = (Map)jsonObject;
		  	Map<String,Object> mapResult = new HashMap<String,Object>();
		  	//参数业务层处理
		  	signService.deleteContract(queryCondition);

		  	mapResult.put("msg","ok");
		  	mapResult.put("flag","ok");
			return mapResult;  
	    }
	/*  @RequestMapping(value = "/deleteAll", method = RequestMethod.GET)
	  public @ResponseBody Map<String,Object> deleteAll(HttpServletRequest request, HttpServletResponse response)
	    {
		  	//参数业务层处理
		    Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
		     String id =  user.get("id").toString();
		  	signService.deleteAllContract(id);
		     
		     Map<String, Object> resultMap = new HashMap<String, Object>();
		     resultMap.put("msg","ok");
		     resultMap.put("flag","ok");
			return resultMap;  
	    }*/
	  
	  //提交审核  一批次将对应  查出 合同数据  合同附件 循环调用
	  @RequestMapping(value = "/submitForView", method = RequestMethod.GET)
	  public @ResponseBody Map<String,Object> submitForView(HttpServletRequest request, HttpServletResponse response)
	    {
		  ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);
		  Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
	      String id =  user.get("id").toString();
		  	 //将附件记录以及
		  Map<String, Object> queryCondition = new HashMap<String, Object>();
		  Map<String,Object> mapResult = new HashMap<String,Object>();
		  queryCondition.put("accountId", id);
		  
		  List<String> errMsg = new ArrayList<String>();
		  List< Map<String, Object>> paramList = new ArrayList< Map<String, Object>>();
		  boolean  b =true;
		  //查询合同  附件记录
		  List<Map<String, Object>> contractsList = signService.querySignContract(queryCondition);
		  if (contractsList.size()==0){ 
			// 无合同信息
			  mapResult.put("flag", "1");
			  b=false;
		  }else if(contractsList.size()>100){
			//合同信息超过100条
			  mapResult.put("flag", "2");
			  b=false;
		  }else{
			  //c_attachment_status中的附件状态是否全部上传
			   List<Map<String, Object>>  noattachs = signService.findContracAttachByStatus("0",id);
			  if(noattachs.size()!=0){
//				  存在未上传的附件
				  b=false;
				  mapResult.put("flag", "4"); //失败 存在附件未上传
				  return mapResult;
			  }
			  	int i = 0;
			   for (Map<String, Object> map : contractsList){
				   i++;
				  //ec_contract_id 去查询合同表看是否存在 存在continue
				  //不存在 继续插入
//				  map.put("accountId", id);
				  map.put("status", "1");
				  //查询ec_contact_id 整个合同表是否存在 存在跳过
				  List<Map<String, Object>> contractsListCreated = signService.querySignContracted(map); 
				if (contractsListCreated.size() == 0){
					Map<String, Object> signResult  = signService.wordTopdf(map,fixedThreadPool);
					paramList.add(signResult);
				}else{
					 b=false;
					 mapResult.put("flag", "3"); //存在相同的合同
					 String msg ="编号为"+map.get("contract_code")+"的合同已存在(ec_contract_id已存在)";
					 errMsg.add(msg);
					 continue;
				}
			} 
		  }
		  //验证全部通过提交审核
		  if(b){
			  signService.updateAndCreateInfo(paramList,id);  
			  mapResult.put("flag", "0");
		  }
		  	mapResult.put("errMsg", errMsg);
			return mapResult;  
	    }
	  
	  //修改mapping信息
	  @RequestMapping(value = "/updateMappingPage")
	  public ModelAndView updateMappingPage(HttpServletRequest request,
			  HttpServletResponse response,
			  @RequestParam(name = "contractId", required = false) String contractId)
	    {
		    // 通过id 查询数据返回页面
		    ModelAndView view = new ModelAndView();
		    view.setViewName("mod_Mapping");  //返回角色设置页面
		    Map<String, Object> queryCondition = new HashMap<String, Object>();//存放参数
		    Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
		     String id =  user.get("id").toString();
		    queryCondition.put("modId",contractId);
		    queryCondition.put("accountId",id);
		    List<Map<String, Object>> list = signService.querySignContract(queryCondition);
		    if ( list.get(0).get("ec_contract_id")!=null)
			{
		    	queryCondition.put("ec_contract_id", list.get(0).get("ec_contract_id"));
			}else{
				queryCondition.put("ec_contract_id", "");
			}
		   
		    List<Map<String, Object>> attahcList = signService.findContracAttachByEcid(queryCondition);
		    
		    view.addObject("attahcList", attahcList);
		    view.addObject("contractMsg", list.get(0));
			return view; 
	    }
	  
	  
	  @RequestMapping(value = "/updateAttach")
	  public @ResponseBody Map<String,Object> updateAttach(HttpServletRequest request, HttpServletResponse response)
	    {
		  	//得参
		  	String paramsStr =  request.getParameter("params") ;
		  	JSONObject jsonObject = JSONArray.parseObject(paramsStr);
		  	Map<String, Object> queryCondition = (Map)jsonObject;
		  	Map<String,Object> mapResult = new HashMap<String,Object>();
		  	//参数业务层处理
		  	signService.updateAttach(queryCondition);

		  	mapResult.put("msg","ok");
		  	mapResult.put("flag","ok");
			return mapResult;  
	    }
	  
	  @RequestMapping(value = "/updateMapping")
	  public @ResponseBody Map<String,Object> updateMapping(HttpServletRequest request, HttpServletResponse response) throws Exception
	    {
		  Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
		    String accountId = user.get("id").toString();
		    String role_id = user.get("role_id").toString();
		    List<String> jzErr = new ArrayList<String>();//匹配不到矩阵
		  	//得参
		  	String paramsStr =  request.getParameter("params") ;
		  	JSONObject jsonObject = JSONArray.parseObject(paramsStr);
		  	Map<String, Object> queryCondition = (Map)jsonObject;
		    Map<String,Object> resultMsg = new HashMap<String,Object>();
		  	//参数业务层处理
		 	String contract_id = queryCondition.get("contract_id").toString();//法人
		 	String old_ec_contract_id = queryCondition.get("old_ec_contract_id").toString();//法人
		    
		  	String legalPerson = queryCondition.get("legal_person").toString();//法人
   		String organization = queryCondition.get("organization").toString();//组织
   		String contractType = queryCondition.get("contract_type").toString();//合同类型
   		String contractLable = queryCondition.get("contract_lable").toString();//合同标签
   		String jdeCode = queryCondition.get("supplier_jde_code").toString();
   		String contract_code = queryCondition.get("contract_code").toString();//合同编号
   		
   		String supplier_name = queryCondition.get("supplier_name").toString();//合同编号
   		//先判断是否存在  这些数据权限 有查出code 拼个联合合同id
   		Map<String, Object> dataAuthMap = new HashMap<String, Object>();
   		dataAuthMap.put("auth_name", "签约法人ALL");
   		dataAuthMap.put("tableName", "legal_person");  
   		dataAuthMap.put("role_id",role_id);
   		dataAuthMap.put("auth_type", "2");//法人 
   		Map<String, Object> legalPersonCode  = signService.queryDateAuthCode(dataAuthMap);
   		
   		dataAuthMap.put("auth_name", "组织类别ALL"); 
   		dataAuthMap.put("tableName", "organization_type");
   		dataAuthMap.put("role_id",role_id);
   		dataAuthMap.put("auth_type", "3");//组织 
   		Map<String, Object> organizationCode  = signService.queryDateAuthCode(dataAuthMap);
   		
   		dataAuthMap.put("auth_name", "合同类型ALL");
   		dataAuthMap.put("tableName", "contract_type");
   		dataAuthMap.put("role_id",role_id);
   		dataAuthMap.put("auth_type", "4");//合同类型
   		Map<String, Object> contractTypeCode  = signService.queryDateAuthCode(dataAuthMap);
   		
   		dataAuthMap.put("auth_name", "合同标签ALL");
   		dataAuthMap.put("tableName", "contract_label");
   		dataAuthMap.put("role_id",role_id);
   		dataAuthMap.put("auth_type", "5");//合同标签
   		Map<String, Object> contractLableCode  = signService.queryDateAuthCode(dataAuthMap);
   		
   		
   		//法人
   		if(legalPersonCode == null){
   			//没有法人ALL权限  
   			//先判断是否存在  这些数据权限 有查出code 拼个联合合同id
   			dataAuthMap.put("auth_name", legalPerson);
   			dataAuthMap.put("tableName", "legal_person");  
   			dataAuthMap.put("role_id",role_id);
   			dataAuthMap.put("auth_type", "2");//法人 
   			legalPersonCode  = signService.queryDateAuthCode(dataAuthMap);
   		}else{
   			dataAuthMap.put("auth_name", legalPerson);
   			dataAuthMap.put("tableName", "legal_person");  
   			legalPersonCode  = signService.queryAllDateAuthCode(dataAuthMap);
   		}
   		
   		//组织
   		if(organizationCode == null){//没有法人ALL权限
   			//先判断是否存在  这些数据权限 有查出code 拼个联合合同id
   			dataAuthMap.put("auth_name", organization); 
   			dataAuthMap.put("tableName", "organization_type");
   			dataAuthMap.put("role_id",role_id);
   			dataAuthMap.put("auth_type", "3");//组织 
   			 organizationCode  = signService.queryDateAuthCode(dataAuthMap);
   		}else{
   			dataAuthMap.put("auth_name", organization);
   			dataAuthMap.put("tableName", "organization_type");  
   			organizationCode  = signService.queryAllDateAuthCode(dataAuthMap);
   		}
   		//合同类型
   		if(contractTypeCode == null){//没有法人ALL权限
   			dataAuthMap.put("auth_name", contractType);
   			dataAuthMap.put("tableName", "contract_type");
   			dataAuthMap.put("role_id",role_id);
   			dataAuthMap.put("auth_type", "4");//合同类型
   			contractTypeCode  = signService.queryDateAuthCode(dataAuthMap);
   		}else{
   			dataAuthMap.put("auth_name", contractType);
   			dataAuthMap.put("tableName", "contract_type");  
   			contractTypeCode  = signService.queryAllDateAuthCode(dataAuthMap);
   		}
   		
   		//合同标签
   		if(contractLableCode == null){//没有法人ALL权限
   			dataAuthMap.put("auth_name", contractLable);
   			dataAuthMap.put("tableName", "contract_label");
   			dataAuthMap.put("role_id",role_id);
   			dataAuthMap.put("auth_type", "5");//合同标签
   			contractLableCode  = signService.queryDateAuthCode(dataAuthMap);
   		}else{
   			dataAuthMap.put("auth_name", contractLable);
   			dataAuthMap.put("tableName", "contract_label");  
   			contractLableCode  = signService.queryAllDateAuthCode(dataAuthMap);
   		}
   		
   		//判断jde是否存在
   		Map<String, Object> jdeObj  = signService.queryJdeCode(jdeCode);
   		
   		Boolean isAuthExist = true;
   		Boolean b  = true;
   		if(legalPersonCode ==null){
   			resultMsg.put("isError", "M");
   			String msg ="签约法人不存在";
   			jzErr.add(msg);
   			isAuthExist = false;
   		} 
   		
   		if(organizationCode ==null){
   			resultMsg.put("isError", "M");
   			String msg ="组织类别不存在";
   			jzErr.add(msg);
   			isAuthExist = false;
   		} 
   		
   		if(contractTypeCode ==null){
   			resultMsg.put("isError", "M");
   			String msg ="合同类型不存在";
   			jzErr.add(msg);
   			isAuthExist = false;
   		} 
   		
   		if(contractLableCode ==null){
   			resultMsg.put("isError", "M");
   			String msg ="合同分类标签不存在";
   			jzErr.add(msg);
   			isAuthExist = false;
   		}
   		
   		//jde是否存在
   		if(jdeObj ==null){ 
   			resultMsg.put("isError", "M");
   			String msg ="jdeCode不存在";
   			jzErr.add(msg);
   			isAuthExist = false;
   		}else{//供应商名称是否一致
   			String supplier_name_biaozhun = jdeObj.get("supplier_name").toString();
   			if(!supplier_name_biaozhun.equals(supplier_name)){
   				resultMsg.put("isError", "M");
   	   			String msg ="供应商名称与jdeCode不一致";
   	   			jzErr.add(msg);
   	   			isAuthExist = false;
   			}
   			
   		}
   		
   		
   		if(isAuthExist == false){
   			b = false;
   		}else{
   			//权限都存在的话 匹配矩阵
   			String ec_contract_id = legalPersonCode.get("code").toString()
   									+organizationCode.get("code").toString()
   									+contractTypeCode.get("code").toString()
   									+jdeObj.get("jde_code").toString()
   									+contract_code;
   			
   			//用ec_contract_id查是否存在合同mapping信息 存在
   			List<Map<String, Object>> contracts = signService.queryContractsbyEcContractId(ec_contract_id);
   			if(contracts.size() !=0&&!contract_id.equals(contracts.get(0).get("id").toString())){
   				String msg ="已存在相同数据权限的合同";
   				jzErr.add(msg); 
   				b = false;
   			}else{
   				int approvalId =	dataManageService.getApprovalBy4Attr(legalPerson, organization, contractType, contractLable);
   				if(approvalId==0){
           			resultMsg.put("isError", "M");
           			String msg = "数据匹配不到审批矩阵";
           			jzErr.add(msg);
           			b = false;
           		} else{
           			queryCondition.put("approval_id", approvalId);
           		}
   			
   			} 
   			queryCondition.put("supplier_account_id", jdeObj.get("account_id").toString());
   			queryCondition.put("ec_contract_id", ec_contract_id);
   			queryCondition.put("legal_person_id",legalPersonCode.get("id").toString()); 
   			queryCondition.put("organization_id",organizationCode.get("id").toString());
   			queryCondition.put("contract_type_id",contractTypeCode.get("id").toString());
   			queryCondition.put("contract_lable_id",contractLableCode.get("id").toString());
   		}
   		
   		if(b){
   			queryCondition.put("old_ec_contract_id", old_ec_contract_id);
   			String deadline = queryCondition.get("deadline").toString();
   			if("".equals(deadline)){
   				queryCondition.put("deadline", null);
   			}
       		signService.updateContractInfo(queryCondition,accountId); 
       		resultMsg.put("isError", "N");
       	}
   		resultMsg.put("jzErr", jzErr);
			return resultMsg;  
	    }
	  
	/***************************zhuyuhua end***********************/







    /****************************zhouzhihui begin****************************/
    /**
     * 跳转发起签约页面
     * @param request           请求
     * @return ModelAndView
     */
    @RequestMapping(value = "/toCreateContract", method = RequestMethod.GET)
    public ModelAndView toCreateContract(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView("sign_create_contract");
        Integer id = new Integer(((Map)request.getSession().getAttribute("current_user")).get("id").toString());
        try {
//            Map<String, Object> map = new HashMap<>();
//            map.put("tableName", "organization_type");
//            map.put("id", id);
//            map.put("authType", "3");
//            List<Map<String, Object>> organizationTypeList = signService.queryContractAttribute(map);
//            map.put("tableName", "contract_type");
//            map.put("authType", "4");
//            List<Map<String, Object>> contractTypeList = signService.queryContractAttribute(map);
//            map.put("tableName", "contract_label");
//            map.put("authType", "5");
//            List<Map<String, Object>> contractLabelList = signService.queryContractAttribute(map);
//            map.put("tableName", "legal_person");
//            map.put("authType", "2");
//            List<Map<String, Object>> legalPersonList = signService.queryContractAttribute(map);
//            List<Map<String, Object>> jdeSupplierList = signService.getAllJdeSupplier();
//            modelAndView.addObject("organizationTypeList", organizationTypeList);
//            modelAndView.addObject("contractTypeList", contractTypeList);
//            modelAndView.addObject("contractLabelList", contractLabelList);
//            modelAndView.addObject("legalPersonList", new Gson().toJson(legalPersonList));
//            modelAndView.addObject("jdeSupplierList", new Gson().toJson(jdeSupplierList));
            List<Integer> authTypeList = signService.selectAllAuthType((Integer)((Map)request.getSession().getAttribute("current_user")).get("id"));
            log.debug("authTypeList : " + authTypeList);
            System.out.println("authTypeList : " + authTypeList);
            if(!authTypeList.containsAll(Arrays.asList(new Integer[]{2,3,4,5}))) {
                modelAndView.addObject("error", "1");
            } else {
                modelAndView.addObject("authTypeList", authTypeList);
            }

        } catch (Exception e) {
            System.out.println("数据库操作异常");
            e.printStackTrace();
            modelAndView.addObject("error", "2");
            modelAndView.setViewName("sign_create_contract");
            MDC.put("input","request");
            MDC.put("output","ModelAndView");
            MDC.put("module","跳转发起签约页面");
            log.info("数据库操作异常");
        }

        return modelAndView;
    }

    /**
     * 查询合同属性数据
     * @param request
     * @param authType
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/queryContractAttribute", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
    public String queryContractAttribute(HttpServletRequest request,
                                         @RequestParam(name = "authType", required = false) Integer authType) {
        Integer id = new Integer(((Map)request.getSession().getAttribute("current_user")).get("id").toString());
        String tableName = null;
        switch (authType) {
            case 2:
                tableName = "legal_person";
                break;
            case 3:
                tableName = "organization_type";
                break;
            case 4:
                tableName = "contract_type";
                break;
            case 5:
                tableName = "contract_label";
                break;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableName);
        map.put("id", id);
        map.put("authType", authType);
        try{
            String result = new Gson().toJson(signService.queryContractAttribute(map));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            MDC.put("input","request");
            MDC.put("output","ModelAndView");
            MDC.put("module","跳转发起签约页面");
            log.info("数据库操作异常");
            return "error";
        }
    }

    @ResponseBody
    @RequestMapping(value = "/querySupplier", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
    public String querySupplier() {

        try{
            String result = new Gson().toJson(signService.getAllJdeSupplier());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            MDC.put("input","");
            MDC.put("output","String");
            MDC.put("module","获取供应商数据");
            log.info("数据库操作异常");
            return "error";
        }
    }

    /**
     * 校验参数，创建合同
     * @param request               请求
     * @param serialNum             合同编号
     * @param ecContractId          ecContractId
     * @param title                 标题
     * @param organizationType     组织类型
     * @param signPlaintext        合同描述
     * @param contractType          合同类型
     * @param contractLabel         合同分类标签
     * @param category               类别
     * @param relatedContractNum    关联合同编号
     * @param baishengContact       合同负责人邮箱
     * @param supplier               供应商
     * @param supplierContact       供应商签约通知邮箱
     * @param legalPerson            签约法人
     * @param file                    主合同文件
     * @param files                   附件文件
     * @return  String
     */
    @ResponseBody
    @RequestMapping(value = "/doCreateContract", method = RequestMethod.POST, produces = "text/html;charset=utf-8")
    public String doCreateContract(HttpServletRequest request,
                                   @RequestParam(name = "serialNum", required = false) String serialNum,
                                   @RequestParam(name = "ecContractId", required = false) String ecContractId,
                                   @RequestParam(name = "title", required = false) String title,
                                   @RequestParam(name = "organizationType", required = false) String organizationType,
                                   @RequestParam(name = "signPlaintext", required = false) String signPlaintext,
                                   @RequestParam(name = "contractType", required = false) String contractType,
                                   @RequestParam(name = "contractLabel", required = false) String contractLabel,
                                   @RequestParam(name = "category", required = false) Integer category,
                                   @RequestParam(name = "relatedContractNum", required = false) String relatedContractNum,
                                   @RequestParam(name = "baishengContact", required = false) String baishengContact,
                                   @RequestParam(name = "supplier", required = false) String[] supplier,
                                   @RequestParam(name = "supplierContact", required = false) String[] supplierContact,
                                   @RequestParam(name = "legalPerson", required = false) String[] legalPerson,
                                   @RequestParam(name = "mainContract", required = false) MultipartFile file,
                                   @RequestParam(name = "mainFile", required = false) String mainFile,
                                   @RequestParam(name = "files", required = false) MultipartFile[] files,
                                   @RequestParam(name = "fileNames", required = false) String[] names,
								   @RequestParam(name = "deadLine", required = false) String deadLineString,
								   @RequestParam(name = "cId", required = false) Integer cId
                                   ){
		StringBuffer message = new StringBuffer();
        try{
			//TODO 校验参数，创建合同
			Integer id = new Integer(((Map)request.getSession().getAttribute("current_user")).get("id").toString());
			ContractEntity contractEntity = null;
			Date deadline = null;
			try {
				if(!StringUtils.isBlank(deadLineString)) {
					deadline = new SimpleDateFormat("yyyy-MM-dd").parse(deadLineString);
				}
			} catch (ParseException e) {
				message.append("截止日期格式有误");
				e.printStackTrace();
			}
			contractEntity = checkParamOfContract(cId, signPlaintext, id, file, mainFile, files, new String[]{serialNum, title, baishengContact, relatedContractNum},
					legalPerson, category, message,  new String[]{organizationType, contractType, contractLabel}, deadline);


			Set<Integer> supplierIds = checkSupplierParams(cId, supplier, supplierContact, message);

	//        if(Constant.CONTRACT_CATEGORY_CANCEL == category) {
	//            if(relatedContractNum == null) {
	//                return "关联合同编号不能为空。";
	//            } else  if(false) {
	//                //TODO 查询编号是否存在，是否是有效状态
	//                return "关联合同编号不存在或不是有效状态。";
	//            } else {
	//                //TODO 处理关联编号
	//            }
	//        }
			try {
				if("".equals(message.toString())) {
					//TODO 校验通过，数据入库
                    StringBuffer jdeCodes = new StringBuffer("");
                    for (String s : supplier) {
                        String[] array = s.split("-");
                        if(array.length == 2 && !StringUtils.isBlank(array[1])) {
                            jdeCodes.append(array[1] + ",");
                        }
                    }
                    contractEntity.setJdeCodes(jdeCodes.deleteCharAt(jdeCodes.length() - 1).toString());
					contractEntity.setSupplierContact(ControllerUtil.convertArrayToString(supplierContact));

					signService.createContract(contractEntity, file, mainFile, files, names, supplierIds);
					return "success";
				}
			} catch (Exception e) {
				e.printStackTrace();
				MDC.put("input","request,serialNum,title,organizationType,signPlaintext,contractType,contractLabel," +
						"category,relatedContractNum,baishengContact,supplierContact,legalPerson,file,files");
				MDC.put("output","String");
				MDC.put("module","创建合约");
				log.info("创建合同异常: " + e.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
			MDC.put("input","request,serialNum,title,organizationType,signPlaintext,contractType,contractLabel," +
					"category,relatedContractNum,baishengContact,supplierContact,legalPerson,file,files");
			MDC.put("output","String");
			MDC.put("module","创建合约");
			log.info("创建合同异常: " + e.getMessage());
			message.append("发生错误");
		}
        return message.toString();
    }

    /**
     * 发起合约参数验证
     * @param userId            用户id
     * @param mainContract     主合同文件
     * @param attachFiles        附件文件数组
     * @param stringParams      字符串型参数数组（传递顺序为：合同编号serialNum, 合同名称title, 合同负责人邮箱baishengContact,
     *                          合同描述signPlaintext, 关联合同编号relatedContractNum）
     * @param legalPersons      签约法人legalPerson数组（传递顺序为：）
     * @param category          合同类别
     * @param message           用于拼接错误信息
     * @param attributes        合同属性描述值数组(顺序：组织类别，合同类型，合同标签)
     * @param extraParams       用于传额外的拼接信息，如批量发起签约需要传入的"第{i}行数据的"，
     *                          只判断是否大于0，并在第一个参数不为null时取第一个参数
     */
    private ContractEntity checkParamOfContract(Integer cId, String signPlaintext, Integer userId, MultipartFile mainContract, String mainFile, MultipartFile[] attachFiles,
                                                String[] stringParams, String[] legalPersons, Integer category,
                                                StringBuffer message, Object[] attributes, Date deadLine, String ... extraParams) {
        String extraString = "";
        Integer organizationType = null, contractType = null, contractLabel = null;
        String legalPerson = "";
        if(extraParams != null && extraParams.length > 0) {
            extraString = (extraParams[0] == null ? "" : extraParams[0]);
        }

        //文件校验：1、主合同是否上传，2、是否有文件名相同，3、文件类型是否正确，4、文件大小是否符合要求
        Set<String> fileNames = new HashSet<>();
        if(!StringUtil.isNull(mainFile))
        {
            System.out.println(mainFile);
        }
        else if(mainContract == null || mainContract.getSize() == 0){
            message.append("请上传" + extraString +"主合同文件。");
        } else if(!mainContract.getOriginalFilename().matches(".*\\.(doc|docx|pdf)")) {
            message.append("主合同要求为doc、docx、pdf格式的文件,主文件：" + mainContract.getOriginalFilename() + "不符合要求。");
        } else if (!checkFile(mainContract, message, extraString)){
            System.out.println(mainContract.getOriginalFilename());
            fileNames.add(mainContract.getOriginalFilename());
        }
        if(attachFiles != null) {
            for (int i = 0; i < attachFiles.length; i++) {
                if(attachFiles[i] != null && attachFiles[i].getSize() !=0) {
                    checkFile(attachFiles[i], message, extraString);
                } else {
                    attachFiles[i] = null;
                }
            }
        }

        boolean isEcContractIdPartitionLegal = true;
        //校验必填参数是否为空，及长度是否超过限制
        //校验合同编号serialNum
        if(StringUtils.isBlank(stringParams[0])) {
            message.append(extraString +"合同编号不能为空。");
            isEcContractIdPartitionLegal = false;
        } else if(stringParams[0].length() > 255) {
            message.append(extraString + "合同编号长度不能超过255。");
            isEcContractIdPartitionLegal = false;
        }

        //校验合同名称title
        if(StringUtils.isBlank(stringParams[1])) {
            message.append(extraString +"合同名称不能为空。");
        } else if(stringParams[1].length() > 50) {
            message.append(extraString +"合同名称长度不能超过50。");
        }

        //校验合同负责人邮箱baishengContact
        if(!StringUtils.isBlank(stringParams[2]) && !stringParams[2].matches(Constant.VALIDATE_EMAIL_PATTERN)) {
            message.append(extraString + "合同负责人邮箱格式有误。");
        }

        //查询合同类别，是特别合同，查询关联合同编号是否存在
        //校验关联合同编号relatedContractNum---------------业务待定
//        if(category == Constant.CONTRACT_CATEGORY_CANCEL) {
//
//        }

        //查询签约法人是否存在
        List<String> legalPersonList = new ArrayList<>();
        String firstLegalPerson = null;
        List<Map<String, Object>> result = null;
        if(legalPersons.length > 0) {
            firstLegalPerson = legalPersons[0];
            for (String s : legalPersons) {
                if(StringUtils.isBlank(s)) {
                    message.append(extraString + "签约法人不能为空。" );
                } else {
                    legalPersonList.add(s);
                }
            }
            if(legalPersonList.size() > 0) {
                result = signService.queryLegalPerson(legalPersonList);
                if(result.size() < legalPersonList.size()) {
                    for (Map<String, Object> s : result) {
                        legalPersonList.remove(s.get("name"));
                    }
                    for (String s : legalPersonList) {
                        message.append(extraString + "签约法人：" + s + "不存在。");
                    }
                }
            }
        } else {
            message.append("请输入" + extraString + "签约法人。");
            isEcContractIdPartitionLegal = false;
        }



        //组装ecContractId，查询是否唯一
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("tableName", "organization_type");
        map.put("id", userId);
        map.put("cId", cId);
        map.put("authType", "3");
        map.put("name", attributes[0]);
        List<Map<String, Object>> organizationTypeResult = signService.queryContractAttribute(map);
        if(organizationTypeResult.size() ==0 ) {
            message.append(extraString + "组织类型不存在或无权限。");
            isEcContractIdPartitionLegal = false;
        } else {
            organizationType = new Integer(organizationTypeResult.get(0).get("id").toString());
        }

        map.put("tableName", "contract_type");
        map.put("authType", "4");
        map.put("name", attributes[1]);
        List<Map<String, Object>> contractTypeResult = signService.queryContractAttribute(map);
        if(contractTypeResult.size() ==0 ) {
            message.append(extraString + "合同类型不存在或无权限。");
            isEcContractIdPartitionLegal = false;
        } else {
            contractType = new Integer(contractTypeResult.get(0).get("id").toString());
        }

        map.put("tableName", "contract_label");
        map.put("authType", "5");
        map.put("name", attributes[2]);
        List<Map<String, Object>> contractLabelResult = signService.queryContractAttribute(map);
        if(contractLabelResult.size() ==0 ) {
            message.append(extraString + "合同分类标签不存在或无权限。");
            isEcContractIdPartitionLegal = false;
        } else {
            contractLabel = new Integer(contractLabelResult.get(0).get("id").toString());
        }

        //按规则拼接ecContractId
        String ecContractId = null;
        if(isEcContractIdPartitionLegal) {
            String legalPseronCode = null;
            for (Map<String, Object> stringStringMap : result) {
                if(firstLegalPerson.equals(stringStringMap.get("name"))) {
                    legalPseronCode = stringStringMap.get("code").toString();
                }
            }
            ecContractId = legalPseronCode + contractLabelResult.get(0). get("code")
                    + contractTypeResult.get(0).get("code") + contractTypeResult.get(0).get("code") + stringParams[0];
            //查询ecContractId是否存在
            if(null == cId)
            {
                cId=0;
            }
            List<String> ecContractIdList = signService.queryEcContractId(ecContractId, cId);
            if(ecContractIdList.size() != 0) {
                message.append(extraString + "合同已存在或者已经创建。");
            }
        }

        //查询审批流是否存在
        Integer approvalId = null;
        if(organizationTypeResult.size() != 0 && contractTypeResult.size() != 0 && contractLabelResult.size() != 0) {
            Map<String, Object> approvalMap = new HashMap<>();
            approvalMap.put("legalPerson", firstLegalPerson);
            approvalMap.put("organizationType", organizationTypeResult.get(0).get("name"));
            approvalMap.put("contractType", contractTypeResult.get(0).get("name"));
            approvalMap.put("contractLabel", contractLabelResult.get(0).get("name"));
            approvalId = signService.queryApproval(approvalMap);
            if(approvalId == null) {
                message.append(extraString + "审批矩阵不存在。");
            }
        }

        if("".equals(message.toString())) {
            //以校验过的参数构造合同实例，并返回
            //TODO 返回后在校验供应商数据无误后需要set供应商邮箱给合同实例对象
            for (Map<String, Object> stringMap : result) {
				legalPerson += stringMap.get("id") + ",";
            }
            legalPerson = legalPerson.substring(0, legalPerson.length() - 1);
            return new ContractEntity(cId, stringParams[0], ecContractId, stringParams[1], userId, stringParams[3],
                                      organizationType, contractType, contractLabel, legalPerson, category + "",
                                      approvalId, stringParams[2], signPlaintext, new Integer(result.get(0).get("id").toString()), deadLine);
        }
        return null;
    }

    /**
     * 校验供应商数据，并返回供应商用用户id列表
     * @param supplier              供应商
     * @param supplierContact      供应商签约通知邮箱
     * @param message               错误信息
     * @param extraParams           额外参数
     * @return
     */
    public Set<Integer> checkSupplierParams(Integer cId, String[] supplier, String[] supplierContact, StringBuffer message, String... extraParams){
        String extraString = "";
        if(extraParams != null && extraParams.length > 0) {
            extraString = (extraParams[0] == null ? "" : extraParams[0]);
        }
        Set<Integer> result = new HashSet<>();
        if(supplier.length > 0) {
            for (String s : supplier) {
                if(!StringUtils.isBlank(s)) {
                    String[] array = s.split("-");
                    boolean flag = false;
                    if (array.length < 2) {
                        message.append(extraString + "供应商" + s + "“不是供应商名-供应商jdeCode”缺失。");
                        flag = true;
                    } else {
                        Map<String, Object> map = new HashMap<>();
                        map.put("supplierName", array[0]);
                        map.put("jdeCode", array[1]);
                        if(StringUtils.isBlank(array[1])) {
                            flag = true;
                        }
                        List<Integer> supplierIds = signService.querySupplierUserId(map);
                        if (supplierIds.size() == 0) {
                            message.append(extraString + "供应商" + s + "不存在。");
                        } else {
                            result.add(supplierIds.get(0));
                        }
                        if(flag){

                        }
                    }
                } else {
                    message.append(extraString + "不是供应商名-供应商jdeCode为空。");
                }
            }
        } else {
            message.append("请输入" + extraString + "供应商名-供应商jdeCode。");
        }

        if(supplierContact.length > 0) {
            for (String s : supplierContact) {
                if(!StringUtils.isBlank(s)) {
                    String[] array = s.split(";");
                    for (String s1 : array) {
                        if (!s1.matches(Constant.VALIDATE_EMAIL_PATTERN)) {
                            message.append(extraString + "供应商签约通知邮箱" + s + "格式有误。");
                        }
                    }
                }
            }
        }

        if(supplierContact.length < supplier.length) {
			message.append(extraString + "供应商签约通知邮箱不能为空。");
		}

        return result;
    }

    /**
     * 校验上传文件的文件名尾缀及文件大小
     * @param file 要校验的文件
     * @param message 错误信息
     * @param extraString 额外参数
     * @return boolean
     */
    private boolean checkFile(MultipartFile file, StringBuffer message, String extraString) {
        boolean result = true;
        if(file.getSize() > 10*1024*1024) {
            message.append(extraString + "文件:" + file.getOriginalFilename() + ",大小超过了10M的限制，。");
            result = false;
        }
        return result;
    }

    @ResponseBody
    @RequestMapping(value = "/getTotalUnsigned", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
    public String getTotalUnsigned(HttpServletRequest request) {
        Integer id = new Integer(((Map)request.getSession().getAttribute("current_user")).get("id").toString());
        return "" + signService.getTotalUnsigned(id);
    }

    @RequestMapping(value = "/queryContract", method = RequestMethod.GET)
	public ModelAndView queryContract(HttpServletRequest request,
											  @RequestParam(name = "pageNum", required = false, defaultValue = "1") Integer pageNum,
											  @RequestParam(name = "start", required = false) String start,
											  @RequestParam(name = "end", required = false) String end,
											  @RequestParam(name = "contractId", required = false) String contractId,
                                              @RequestParam(name = "status", required = false) String status,
											  @RequestParam(name = "title", required = false) String title){
		ModelAndView modelAndView = new ModelAndView();
        if("1".equals(status)) {
            modelAndView.setViewName("contractsignature_signature_yes");
        } else {
            modelAndView.setViewName("contractsignature_signature_no");
        }
		Integer id = new Integer(((Map)request.getSession().getAttribute("current_user")).get("id").toString());
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("status", status);
        map.put("start", start);
        map.put("end", ControllerUtil.dealWithEndDate(end));
        map.put("contractId", contractId);
        map.put("title", title);
        List<Map<String, Object>> contractList = signService.queryContract(map, new RowBounds(pageNum, Constant.PAGE_MAX));
        PageInfo<Map<String, Object>> data = new PageInfo<>(contractList);
        modelAndView.addObject("data", data);
        String url = ControllerUtil.assembleUrlWithParams(request, map);
        modelAndView.addObject("url", url);
		return modelAndView;
	}

    @RequestMapping(value = "/toSignContract", method = RequestMethod.GET, produces = "text/plain;charset=utf-8")
    public ModelAndView toSignContract(HttpServletRequest request,
                                       @RequestParam(name = "contractId", required = false) Integer contractId) {
        ModelAndView modelAndView = new ModelAndView("sign_contract");
		Integer id = new Integer(((Map)request.getSession().getAttribute("current_user")).get("id").toString());
        Map<String, Object> data = signService.queryUnsignedContractById(contractId);
        modelAndView.addObject("plainText", data.get("plainText"));
        //TODO pdf全部截图地址返回页面，显示
        String projectDir = ContextLoader.getCurrentWebApplicationContext().getServletContext().getRealPath("/");
        String contractPath = (String)data.get("contractPath");
        String filePath = (String)data.get("filePath");
        String imgPath = contractPath + "img/" + filePath.substring(contractPath.length(), filePath.length() - 4).replaceAll("pdfsign/", "");
        String imgDir = projectDir  + imgPath;
        System.out.println("imgDir : " + imgDir);
        File dir = new File(imgDir);
        System.out.println(dir.getAbsolutePath());
        String[] fileNames = dir.list();

        System.out.println("fileNames : " + Arrays.deepToString(fileNames));
        modelAndView.addObject("imgPath", imgPath);
        modelAndView.addObject("fileNames", fileNames);
		Gson g = new Gson();
        Map<String, Object> map = new HashMap<>();
        map.put("id", contractId);
        map.put("status", 0);
        List<Map<String, Object>> list = signService.queryContractById(map);
        if(list.size() > 0) {
            modelAndView.addObject("contract", list.get(0));
            modelAndView.addObject("approvaledUsers", g.toJson(contractManageService.getTraceProcess(contractId)));
        } else {
            modelAndView.setViewName("redirect:/sign/queryContract.do?status=0");
        }
        return modelAndView;
    }

    @ResponseBody
	@RequestMapping(value = "/doSignContract", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
	public String doSignContract(HttpServletRequest request,
                                       @RequestParam(name = "imgData", required = false) String imgData,
									   @RequestParam(name = "contractId", required = false) Integer contractId,
                                 //保存为cert
                                       @RequestParam(name = "certContent", required = false) String certContent,
                                 //指纹，用于获取时间戳
                                       @RequestParam(name = "certThumbPrint", required = false) String certThumbPrint,
                                 //主题，暂无用处
                                       @RequestParam(name = "certSubject", required = false) String certSubject,
                                 //暂无用处
                                       @RequestParam(name = "certBeforeSystemTime", required = false) String certBeforeSystemTime,
                                 //暂无用处
                                       @RequestParam(name = "certAfterSystemTime", required = false) String certAfterSystemTime,
                                 //颁发者，暂无用处
                                       @RequestParam(name = "certIssuer", required = false) String certIssuer,
                                       @RequestParam(name = "signData", required = false) String signData,
                                       @RequestParam(name = "isAgreed", required = false) String isAgreed) {

        if("1".equals(isAgreed)) {
            System.out.println(imgData);
            //同意签署，完成签署过程
            Map<String, Object>  user =  (Map<String, Object>) request.getSession().getAttribute("current_user");
            Integer id = (Integer)user.get("id");
            signService.supplierSign(id, contractId, certContent, certThumbPrint, signData, imgData.replaceAll("/sharefile/yunsign/image/(demo\\d\\.gif)", ContextLoader.getCurrentWebApplicationContext().getServletContext().getRealPath("/").replaceAll("\\\\", "/") + "/images/$1"));
        } else if("0".equals(isAgreed)) {
            //签署拒绝
            Map<String, Object> map = new HashMap<>();
            map.put("id", contractId);
            map.put("status", Constant.CONTRACT_STATUS_SIGN_REFUSED);
            signService.rejectSign(map);
        } else {
            return "请选择是否同意!";
        }

		return "";
	}
    /****************************zhouzhihui end****************************/


    /****************************qiyusheng begin****************************/
    @ResponseBody
    @RequestMapping(value="getTotalForUnApproval.do", produces = "text/plain;charset=utf-8")
    public String getTotalForUnApproval(HttpServletRequest req, HttpServletResponse rsp)
    {
        Map<String, Object>  user =  (Map<String, Object>) req.getSession().getAttribute("current_user");
        Integer id = (Integer)user.get("id");
        
        Integer total = signService.getTotalForNoApproval(id);

        return String.valueOf(total);
    }

    @RequestMapping(value="getApprovalInfo.do", produces = "text/plain;charset=utf-8")
    public ModelAndView getApprovalInfo(HttpServletRequest req, HttpServletResponse rsp)
    {
        ModelAndView modelAndView = new ModelAndView();
        List<Map<String, Object>> infolist = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        List<Integer> list = new ArrayList<Integer>();
        String status = req.getParameter("status");
        try
        {
            Map<String, Object> user = (Map<String, Object>)req.getSession().getAttribute("current_user");
            Integer userId = (Integer)user.get("id");
            if(StringUtil.isNull(status) || Constant.ZERO == Integer.valueOf(status))
            {
                modelAndView.setViewName("contractsignature_approval_no");
                list.add(Constant.UN_APPROVAL);
                map.put("status", list);
            }
            else
            {
                modelAndView.setViewName("contractsignature_approval_yes");
                list.add(Constant.AGREE_APPROVAL);
                list.add(Constant.BACK_APPROVAL);
                list.add(Constant.REFUSE_APPROVAL);
                list.add(Constant.FINISH_APPROVAL);
                map.put("status", list);
            }
            String id = req.getParameter("id");
            String name = req.getParameter("name");
            String start = req.getParameter("start");
            String end = req.getParameter("end");
            String legalPerson = req.getParameter("legalPerson");
            String category = req.getParameter("category");
            String supplierName = req.getParameter("supplierName");
            String jdeCode = req.getParameter("jdeCode");           
            String baishengContact = req.getParameter("baishengContact");
            String supplierContact = req.getParameter("supplierContact");

            map.put("userId", userId);
            map.put("constractId", id);
            map.put("contractName", name);
            map.put("startTime", start);
            map.put("endTime", end);
            map.put("legalPerson", legalPerson);
            map.put("category", category);
            map.put("supplierName", supplierName);
            map.put("jdeCode", jdeCode);
            map.put("baishengContact", baishengContact);
            map.put("supplierContact", supplierContact);

            
            req.setAttribute("constractId", id);
            req.setAttribute("contractName", name);
            req.setAttribute("startTime", start);
            req.setAttribute("endTime", end);
            req.setAttribute("legalPerson", legalPerson);
            req.setAttribute("category", category);
            req.setAttribute("supplierName", supplierName);
            req.setAttribute("jdeCode", jdeCode);
            req.setAttribute("baishengContact", baishengContact);
            req.setAttribute("supplierContact", supplierContact);
            String pageNum = req.getParameter("pageNum");
            String page = req.getParameter("pageQuery");
            
            RowBounds rb;
            if (StringUtil.isNull(pageNum))
            {
                rb = new RowBounds(Constant.PAGE_NUM, Constant.PAGE_MAX);
                modelAndView.addObject("url", req.getRequestURI() + "?");
            }
            else
            {
                rb = new RowBounds(Integer.parseInt(pageNum), Constant.PAGE_MAX);
            }
            if (!StringUtil.isNull(page))
            {
                StringBuffer sb = new StringBuffer();
                sb.append(req.getRequestURI());
                sb.append("?pageQuery=1&id=");
                sb.append(id);
                sb.append("&name=");
                sb.append(name);
                sb.append("&start=");
                sb.append(start);
                sb.append("&end=");
                sb.append(end);
                sb.append("&status=");
                sb.append(status);
                sb.append("&");
                System.out.println(sb.toString());
                modelAndView.addObject("url", sb.toString());
            }
            else
            {
                modelAndView.addObject("url", req.getRequestURI() + "?");
            }
            infolist = signService.getUnApprovalInfo(map, rb);
            PageInfo<Map<String, Object>> data = new PageInfo<Map<String, Object>>(infolist);
            System.out.println(data);
            modelAndView.addObject("data", data);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return modelAndView;
    }
    
    @ResponseBody
    @RequestMapping(value="beginToApproval.do", produces = "text/plain;charset=utf-8")
    public String beginToApproval(HttpServletRequest req, HttpServletResponse rsp)
    {
        String oId = req.getParameter("orderid");
        String conId = req.getParameter("cId");
        int cId = Integer.parseInt(conId);
        int orderId = Integer.parseInt(oId);
        int result = 0;
        try
        {
            result = dataManageService.exceptionContract(cId, orderId);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(Constant.ZERO != result)
        {
            return "合同异常，无法审批";
        }
        return "success";
    }
    
    @RequestMapping(value="getApprovalFollowing.do", produces = "text/plain;charset=utf-8")
    public String getApprovalFollowing(HttpServletRequest req, HttpServletResponse rsp)
    {
        Gson g = new Gson();
        String oId = req.getParameter("orderid");
        String flowId = req.getParameter("flowId");
        String cId = req.getParameter("cId");
        int isHidden = Constant.ZERO;
        int id = Integer.parseInt(cId);
        try
        {
            if(String.valueOf(Constant.ONE).equals(req.getParameter("checkId")))
            {
                isHidden=Constant.ONE;
            }
            Map<String, Object> contractInfo = signService.getContractInfoByCId(id);
            List<Map<String, Object>> fileList = signService.getContractAttachmentInfoByCId(id);
            req.setAttribute("approvaledUsers", g.toJson(contractManageService.getTraceProcess(id)));
            req.setAttribute("contractInfo", contractInfo);
            req.setAttribute("fileList", fileList);
            req.setAttribute("flowId", flowId);
            req.setAttribute("orderId", oId);
            req.setAttribute("cId", cId);
            req.setAttribute("isHidden", isHidden);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "contractsignature_approval_look";
    }

    @RequestMapping(value="approvalResultHandle.do", produces = "text/plain;charset=utf-8")
    public String approvalResultHandle(HttpServletRequest req, HttpServletResponse rsp)
    {
        String result = req.getParameter("result");
        String flowId = req.getParameter("flowId");
        String value = req.getParameter("cId");
        String suggest = req.getParameter("suggest");
        String orderId = req.getParameter("orderId");
        List<String> ids = Arrays.asList(value.split(","));
        List<Integer> idList = new ArrayList<Integer>();
        List<ApprovalFlowEntity> entityList = new ArrayList<ApprovalFlowEntity>();
        Map<String, Object> map = new HashMap<String, Object>();
        ApprovalFlowEntity entity = new ApprovalFlowEntity();
        Map<String, Object> user = (Map<String, Object>)req.getSession().getAttribute("current_user");
        Integer userId = (Integer)user.get("id");
        Date date = new Date();

        for(String str : ids)
        {
            idList.add(Integer.parseInt(str));
        }
        for(int contractId : idList)
        {
            entity.setId(Integer.parseInt(flowId));
            entity.setContractId(contractId);
            entity.setApprovalTime(date);
            entity.setApprovalUserId(userId);
            entity.setApprovalOrderId(Integer.parseInt(orderId));
            entity.setApprovalSuggest(suggest);
            if(String.valueOf(Constant.AGREE_APPROVAL).equals(result))
            {
                entity.setApprovalStatus(Constant.AGREE_APPROVAL);
            }
            else if(String.valueOf(Constant.BACK_APPROVAL).equals(result))
            {
                entity.setApprovalStatus(Constant.BACK_APPROVAL);
            }
            else
            {
                entity.setApprovalStatus(Constant.REFUSE_APPROVAL);
            }
            entityList.add(entity);
        }
        
        signService.approvalResultHandle(entityList, userId);

        return "forward:getApprovalInfo.do";
    }
    
    @ResponseBody
    @RequestMapping(value="batchApprovalAgree.do", produces = "text/plain;charset=utf-8")
    public String batchApprovalAgree(HttpServletRequest req, HttpServletResponse rsp)
    {
        List<ApprovalFlowEntity> entityList = new ArrayList<ApprovalFlowEntity>();

        List<Integer> idList = new ArrayList<Integer>();
        String suggest = req.getParameter("suggest");
        Map<String, Object> user = (Map<String, Object>)req.getSession().getAttribute("current_user");
        Integer userId = (Integer)user.get("id");
        Date date = new Date();
        String[] flowId = req.getParameterValues("ids");
        String[] orderId = req.getParameterValues("orderid");
        String[] cIds = req.getParameterValues("cid");
        try
        {
        for(int i=0; i<flowId.length; i++)
        {
            ApprovalFlowEntity entity = new ApprovalFlowEntity();
            entity.setContractId(Integer.parseInt(cIds[i]));
            entity.setApprovalOrderId(Integer.parseInt(orderId[i]));
            entity.setId(Integer.parseInt(flowId[i]));
            
            entity.setApprovalSuggest(suggest);
            entity.setApprovalUserId(userId);
            entity.setApprovalTime(date);
            entity.setApprovalStatus(Constant.AGREE_APPROVAL);
            entityList.add(entity);
        }

        signService.approvalResultHandle(entityList, userId);
        }
        catch(Exception e)
        {
            log.error("batch approval agree success...");
            return "批量审批同意失败";
        }
        return "批量审批同意成功";
    }
    
    @RequestMapping(value="getApprovalProcess.do", produces = "text/plain;charset=utf-8")
    public String getApprovalProcess(HttpServletRequest req, HttpServletResponse rsp)
    {
        Gson g = new Gson();
        String conId = req.getParameter("conId");
        int cId = Integer.parseInt(conId);
        List<Map<String, Object>> list = signService.getApprovalFollowedByContractId(cId);
        Map<String, Object> contractInfo = signService.getContractInfoByCId(cId);
        int size = list.size();
        List<Integer> userIds = new ArrayList<Integer>();
        List<Integer> orderIds = new ArrayList<Integer>();
        Map<String, Object> queryMap = new HashMap<String, Object>();
        List<Map<String, Object>> nodeList = signService.getNodeInfoByCid(cId);

        Map<String, Integer> order = signService.getFlowLastOrderIdbyCid(cId);
        int orderId = order.get("orderId");
        if(Constant.UN_APPROVAL == order.get("status") )
        {
            size=list.size()-1;
        }
        
        for (int i = orderId; i < nodeList.size()-1; i++)
        {
            userIds.add((Integer)nodeList.get(i).get("approvalUserId"));
            orderIds.add((Integer)nodeList.get(i).get("approvalOrderId"));
        }
        queryMap.put("userIds", userIds);
        queryMap.put("orderIds", orderIds);
        queryMap.put("approvalflowName", nodeList.get(0).get("approvalflowName"));
        if(Constant.ZERO != userIds.size())
        {
            List<Map<String, Object>> accountList = dataManageService.getIdentityUserByIds(queryMap);
            for(Map<String, Object> map:accountList)
            {
                Map<String, Object> flowInfo = new HashMap<String, Object>();
                flowInfo.put("name", map.get("name"));
                flowInfo.put("status", Constant.UN_APPROVAL);
                
                list.add(flowInfo);
               // unApprovalUsers.add((String)map.get("account"));
            }
        }
        req.setAttribute("list", g.toJson(list));
        req.setAttribute("size", size);
        req.setAttribute("crontractId", contractInfo.get("contractId"));
        req.setAttribute("title", contractInfo.get("title"));
        
        return "contractsignature_approval_ process";
    }
    
    @RequestMapping(value="recycleBins.do", produces = "text/plain;charset=utf-8")
    public ModelAndView recycleBins(HttpServletRequest req, HttpServletResponse rsp)
    {
        ModelAndView modelAndView = new ModelAndView();
        Map<String, Object> map = new HashMap<String, Object>();
        try
        {
            modelAndView.setViewName("sign_box");
            Map<String, Object> user = (Map<String, Object>)req.getSession().getAttribute("current_user");
            Integer userId = (Integer)user.get("id");

            String id = req.getParameter("id");
            String name = req.getParameter("name");
            String start = req.getParameter("start");
            String end = req.getParameter("end");
            
            map.put("userId", userId);
            map.put("constractId", id);
            map.put("contractName", name);
            map.put("startTime", start);
            map.put("endTime", end);
            
            req.setAttribute("constractId", id);
            req.setAttribute("contractName", name);
            req.setAttribute("startTime", start);
            req.setAttribute("endTime", end);
            
            String pageNum = req.getParameter("pageNum");
            String page = req.getParameter("pageQuery");
            
            RowBounds rb;
            if (StringUtil.isNull(pageNum))
            {
                rb = new RowBounds(Constant.PAGE_NUM, Constant.PAGE_MAX);
                modelAndView.addObject("url", req.getRequestURI() + "?");
            }
            else
            {
                rb = new RowBounds(Integer.parseInt(pageNum), Constant.PAGE_MAX);
            }
            if (!StringUtil.isNull(page))
            {
                StringBuffer sb = new StringBuffer();
                sb.append(req.getRequestURI());
                sb.append("?pageQuery=1&id=");
                sb.append(id);
                sb.append("&name=");
                sb.append(name);
                sb.append("&start=");
                sb.append(start);
                sb.append("&end=");
                sb.append(end);
                sb.append("&");
                System.out.println(sb.toString());
                modelAndView.addObject("url", sb.toString());
            }
            else
            {
                modelAndView.addObject("url", req.getRequestURI() + "?");
            }
            List<Map<String, Object>> infolist = signService.getContractByCidAndCreator(map, rb);
            PageInfo<Map<String, Object>> data = new PageInfo<Map<String, Object>>(infolist);
            System.out.println(data);
            modelAndView.addObject("data", data);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return modelAndView;
    }
    
    @ResponseBody
    @RequestMapping(value="cancelContract.do", produces = "text/plain;charset=utf-8")
    public String cancelContract(HttpServletRequest req, HttpServletResponse rsp)
    {
        String cId = req.getParameter("cId");
        try
        {
            int id = Integer.parseInt(cId);
            
            signService.cancelContractbyId(id);
        }
        catch(Exception e)
        {
            return "取消合同失败";
        }
        
        return "取消合同成功";
    }

    @RequestMapping(value="reCreateContract.do", produces = "text/plain;charset=utf-8")
    public String reCreateContract(HttpServletRequest req, HttpServletResponse rsp)
    {
        Gson gson = new Gson();
        String cId = req.getParameter("conId");
        try
        {
            int id = Integer.parseInt(cId);
            
            Map<String, Object> map = signService.reCreateContractById(id);
            String str = (String)map.get("legalPerson");
            List<String> list = Arrays.asList(str.split(","));
            List<String> user = signService.getLegalPersonById(list);
            
            List<String> nameList = new ArrayList<String>();
            Map<String, Object> nameMap = new HashMap<String, Object>();
            nameMap.put("id", id);
            nameMap.put("name", nameList);

            List<Map<String, Object>> pathList = signService.getOriginalFilePathById(nameMap);
            for(int i=0;i<pathList.size();i++)
            {
                nameList.add((String)pathList.get(i).get("originalName"));
            }
            
            req.setAttribute("user", gson.toJson(user));
            req.setAttribute("map", map);
            req.setAttribute("nameList", gson.toJson(nameList));
        }
        catch(Exception e)
        {
            e.printStackTrace();
            req.setAttribute("result", "result");
            return "sign_box";
        }
        
        return "sign_create_contract";
    }
    
    @ResponseBody
    @RequestMapping(value="downloadFile.do", produces = "text/plain;charset=utf-8")
    public String downloadFile(HttpServletRequest req, HttpServletResponse rsp)
    {
        String fileName = req.getParameter("fileName");
        String cId = req.getParameter("cId");
        try
        {
            int id = Integer.parseInt(cId);
            List<Map<String, Object>> fileList = signService.getContractAttachmentInfoByCId(id);
            for(Map<String, Object> map :fileList)
            {
                if(Constant.ONE != (Integer)map.get("type") && fileName.equals((String)map.get("fileName")))
                {
                    String path = (String)map.get("filePath");
                    
                    File file = new File(path);
                    
                    if(file.exists())
                    {
                        System.out.println("exists file");
                    }
                    // 取得文件名。
                    String filename = file.getName();
                    InputStream is = new FileInputStream(file); 
                    // 以流的形式下载文件。
                    InputStream fis = new BufferedInputStream(is);
                    byte[] buffer = new byte[fis.available()];
                    fis.read(buffer);
                    fis.close();
                    // 清空response
                    rsp.reset();
                    // 设置response的Header
                    rsp.addHeader("Content-Disposition", "attachment;filename=" + new String(filename.getBytes()));
                    rsp.addHeader("Content-Length", "" + file.length());
                    OutputStream toClient = new BufferedOutputStream(rsp.getOutputStream());
                    rsp.setContentType("application/octet-stream");
                    toClient.write(buffer);
                    toClient.flush();
                    toClient.close();
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return "";
        }
        return "";
    }
	/****************************qiyusheng end****************************/
}
