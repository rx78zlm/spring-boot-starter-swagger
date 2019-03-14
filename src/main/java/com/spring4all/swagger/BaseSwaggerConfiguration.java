package com.spring4all.swagger;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.spring4all.swagger.properties.DocketInfo;
import com.spring4all.swagger.properties.GlobalOperationParameter;
import com.spring4all.swagger.properties.GlobalResponseMessageBody;
import com.spring4all.swagger.properties.SwaggerProperties;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.*;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Created by zhangleimin on 2019/3/11.
 */
public class BaseSwaggerConfiguration {

    protected Docket configPath(DocketInfo docketInfo, ApiSelectorBuilder apiSelectorBuilder) throws ClassNotFoundException {
        // base-path处理
        // 当没有配置任何path的时候，解析/**
        if (docketInfo.getBasePath().isEmpty()) {
            docketInfo.getBasePath().add("/**");
        }
        List<Predicate<String>> basePath = new ArrayList<>();
        for (String path : docketInfo.getBasePath()) {
            basePath.add(PathSelectors.ant(path));
        }
        // exclude-path处理
        List<Predicate<String>> excludePath = new ArrayList<>();
        for (String path : docketInfo.getExcludePath()) {
            excludePath.add(PathSelectors.ant(path));
        }
        Docket docket = apiSelectorBuilder.apis(RequestHandlerSelectors.basePackage(docketInfo.getBasePackage()))
                .paths(
                        // 满足在basePath中并且不能在excludePath列表中的路径才能被访问
                        Predicates.and(
                                Predicates.not(Predicates.or(excludePath)),
                                Predicates.or(basePath)
                        )
                ).build()
                .pathMapping(docketInfo.getPathMapping());
        /* 忽略不被处理的参数类型 **/
        Class<?>[] array = new Class[docketInfo.getIgnoredParameterTypes().size()];
        Class<?>[] ignoredParameterTypes = docketInfo.getIgnoredParameterTypes().toArray(array);
        docket.ignoredParameterTypes(ignoredParameterTypes);
        if (!Strings.isNullOrEmpty(docketInfo.getDirectModelSubstitutes())) {
            setDirectModelSubstitutes(docket, docketInfo.getDirectModelSubstitutes());
        }
        return docket;
    }

    private void setDirectModelSubstitutes(Docket docket, String directModelSubstitutes) throws ClassNotFoundException {
        Map<String, String> map = Splitter.on(',').withKeyValueSeparator('-').split(directModelSubstitutes);
        for (String className : map.keySet()) {
            String with = map.get(className);
            Class clazz = ClassUtils.forName(className, null);
            Class withClazz = ClassUtils.forName(with, null);
            docket.directModelSubstitute(clazz, withClazz);
        }
    }

    protected ApiInfo createApiInfo(DocketInfo defaultDocketInfo, DocketInfo docketInfo) {
        DocketInfo docket = Optional.ofNullable(docketInfo).orElse(defaultDocketInfo);
        return new ApiInfoBuilder()
                .title(docket.getTitle().isEmpty() ? defaultDocketInfo.getTitle() : docket.getTitle())
                .description(docket.getDescription().isEmpty() ? defaultDocketInfo.getDescription() : docket.getDescription())
                .version(docket.getVersion().isEmpty() ? defaultDocketInfo.getVersion() : docket.getVersion())
                .license(docket.getLicense().isEmpty() ? defaultDocketInfo.getLicense() : docket.getLicense())
                .licenseUrl(docket.getLicenseUrl().isEmpty() ? defaultDocketInfo.getLicenseUrl() : docket.getLicenseUrl())
                .contact(
                        new Contact(
                                docket.getContact().getName().isEmpty() ? defaultDocketInfo.getContact().getName() : docket.getContact().getName(),
                                docket.getContact().getUrl().isEmpty() ? defaultDocketInfo.getContact().getUrl() : docket.getContact().getUrl(),
                                docket.getContact().getEmail().isEmpty() ? defaultDocketInfo.getContact().getEmail() : docket.getContact().getEmail()
                        )
                )
                .termsOfServiceUrl(docket.getTermsOfServiceUrl().isEmpty() ? defaultDocketInfo.getTermsOfServiceUrl() : docket.getTermsOfServiceUrl())
                .build();
    }

    protected List<Parameter> buildGlobalOperationParametersFromSwaggerProperties(
            List<GlobalOperationParameter> globalOperationParameters) {
        List<Parameter> parameters = newArrayList();

        if (Objects.isNull(globalOperationParameters)) {
            return parameters;
        }
        for (GlobalOperationParameter globalOperationParameter : globalOperationParameters) {
            parameters.add(new ParameterBuilder()
                    .name(globalOperationParameter.getName())
                    .description(globalOperationParameter.getDescription())
                    .modelRef(new ModelRef(globalOperationParameter.getModelRef()))
                    .parameterType(globalOperationParameter.getParameterType())
                    .required(Boolean.parseBoolean(globalOperationParameter.getRequired()))
                    .build());
        }
        return parameters;
    }

    /**
     * 局部参数按照name覆盖局部参数
     *
     * @param globalOperationParameters
     * @param docketOperationParameters
     * @return
     */
    protected List<Parameter> assemblyGlobalOperationParameters(
            List<GlobalOperationParameter> globalOperationParameters,
            List<GlobalOperationParameter> docketOperationParameters) {

        if (Objects.isNull(docketOperationParameters) || docketOperationParameters.isEmpty()) {
            return buildGlobalOperationParametersFromSwaggerProperties(globalOperationParameters);
        }

        Set<String> docketNames = docketOperationParameters.stream()
                .map(GlobalOperationParameter::getName)
                .collect(Collectors.toSet());

        List<GlobalOperationParameter> resultOperationParameters = newArrayList();

        // 全局参数不为空
        if (Objects.nonNull(globalOperationParameters)) {
            for (GlobalOperationParameter parameter : globalOperationParameters) {
                if (!docketNames.contains(parameter.getName())) {
                    // 将分组项中的全局配置也添加到全局配置参数中
                    resultOperationParameters.add(parameter);
                }
            }
        }

        resultOperationParameters.addAll(docketOperationParameters);
        return buildGlobalOperationParametersFromSwaggerProperties(resultOperationParameters);
    }

    /**
     * 设置全局响应消息
     *
     * @param swaggerProperties swaggerProperties 支持 POST,GET,PUT,PATCH,DELETE,HEAD,OPTIONS,TRACE
     * @param docketForBuilder  swagger docket builder
     */
    protected void buildGlobalResponseMessage(SwaggerProperties swaggerProperties, Docket docketForBuilder) {
        docketForBuilder.useDefaultResponseMessages(swaggerProperties.getApplyDefaultResponseMessages());
        // 使用默认http响应则直接返回
        if (swaggerProperties.getApplyDefaultResponseMessages()) {
            return;
        }
        SwaggerProperties.GlobalResponseMessage globalResponseMessages =
                swaggerProperties.getGlobalResponseMessage();

        /* POST,GET,PUT,PATCH,DELETE,HEAD,OPTIONS,TRACE 响应消息体 **/
        List<ResponseMessage> postResponseMessages = getResponseMessageList(globalResponseMessages.getPost());
        List<ResponseMessage> getResponseMessages = getResponseMessageList(globalResponseMessages.getGet());
        List<ResponseMessage> putResponseMessages = getResponseMessageList(globalResponseMessages.getPut());
        List<ResponseMessage> patchResponseMessages = getResponseMessageList(globalResponseMessages.getPatch());
        List<ResponseMessage> deleteResponseMessages = getResponseMessageList(globalResponseMessages.getDelete());
        List<ResponseMessage> headResponseMessages = getResponseMessageList(globalResponseMessages.getHead());
        List<ResponseMessage> optionsResponseMessages = getResponseMessageList(globalResponseMessages.getOptions());
        List<ResponseMessage> trackResponseMessages = getResponseMessageList(globalResponseMessages.getTrace());

        docketForBuilder.useDefaultResponseMessages(swaggerProperties.getApplyDefaultResponseMessages())
                .globalResponseMessage(RequestMethod.POST, postResponseMessages)
                .globalResponseMessage(RequestMethod.GET, getResponseMessages)
                .globalResponseMessage(RequestMethod.PUT, putResponseMessages)
                .globalResponseMessage(RequestMethod.PATCH, patchResponseMessages)
                .globalResponseMessage(RequestMethod.DELETE, deleteResponseMessages)
                .globalResponseMessage(RequestMethod.HEAD, headResponseMessages)
                .globalResponseMessage(RequestMethod.OPTIONS, optionsResponseMessages)
                .globalResponseMessage(RequestMethod.TRACE, trackResponseMessages);
    }

    /**
     * 获取返回消息体列表
     *
     * @param globalResponseMessageBodyList 全局Code消息返回集合
     * @return
     */
    private List<ResponseMessage> getResponseMessageList
    (List<GlobalResponseMessageBody> globalResponseMessageBodyList) {
        List<ResponseMessage> responseMessages = new ArrayList<>();
        for (GlobalResponseMessageBody globalResponseMessageBody : globalResponseMessageBodyList) {
            ResponseMessageBuilder responseMessageBuilder = new ResponseMessageBuilder()
                    .code(globalResponseMessageBody.getCode())
                    .message(globalResponseMessageBody.getMessage());

            if (!StringUtils.isEmpty(globalResponseMessageBody.getModelRef())) {
                responseMessageBuilder.responseModel(new ModelRef(globalResponseMessageBody.getModelRef()));
            }
            responseMessages.add(responseMessageBuilder.build());
        }

        return responseMessages;
    }
}
