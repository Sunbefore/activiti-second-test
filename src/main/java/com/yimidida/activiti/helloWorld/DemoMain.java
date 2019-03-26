package com.yimidida.activiti.helloWorld;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DemoMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoMain.class);

    public static void main(String[] args) throws ParseException {
        LOGGER.info("启动我们的程序");
        // 创建流程引擎
        ProcessEngineConfiguration config = ProcessEngineConfiguration.
                createProcessEngineConfigurationFromResource("activiti.cfg.xml");

        ProcessEngine engine = config.buildProcessEngine();
        String name = engine.getName();
        String version = ProcessEngine.VERSION;
        LOGGER.info("流程引擎的name:{}, version:{}", name, version);

        // 部署流程文件
        RepositoryService repositoryService = engine.getRepositoryService();
        DeploymentBuilder builder = repositoryService.createDeployment();
        // 避免重复部署enableDuplicateFiltering
        Deployment deploy = builder.addClasspathResource("second_approve.bpmn20.xml")
            .deploy();//.enableDuplicateFiltering().deploy();
        String deploymentId = deploy.getId();

        // 获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
        LOGGER.info("流程定义文件:{}, 流程ID:{}", processDefinition.getName(), processDefinition.getId());

        // 启动流程
        RuntimeService runtimeService = engine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        LOGGER.info("启动流程:{}", processInstance.getProcessDefinitionKey());

        // 处理流程任务
        Scanner scanner = new Scanner(System.in);
        // 如果流程实例不为空且流程实例没有结束
        while (processInstance != null && !processInstance.isEnded()){

            TaskService taskService = engine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            LOGGER.info("待处理任务数量:{}", list.size());
            for(Task task : list){
                LOGGER.info("待处理任务:{}", task.getName());
                FormService formService = engine.getFormService();
                TaskFormData taskFormData = formService.getTaskFormData(task.getId());
                List<FormProperty> formProperties = taskFormData.getFormProperties();

                Map<String, Object> variables = Maps.newHashMap();
                for (FormProperty formProperty : formProperties){
                    LOGGER.info("请输入：{} ,若是时间请输入(yyyy-MM-dd)", formProperty.getName());
                    String line = scanner.nextLine();
                    // 字符串类型
                    if(StringFormType.class.isInstance(formProperty.getType())){
                        // LOGGER.info("请输入：{}", formProperty.getName());
                        variables.put(formProperty.getId(), line);
                    }
                    // 日期类型
                    else if (DateFormType.class.isInstance(formProperty.getType())){

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = simpleDateFormat.parse(line);
                        variables.put(formProperty.getId(), date);
                    }else{
                        LOGGER.info("类型暂时不支持{}：", formProperty.getType());
                    }
                    LOGGER.info("您输入的内容是：{}", line);

                }
                taskService.complete(task.getId(), variables);
                // 重新获取流程实例对象
                processInstance = engine.getRuntimeService().createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();

            }

        }



        LOGGER.info("结束我们的程序");
    }
}
