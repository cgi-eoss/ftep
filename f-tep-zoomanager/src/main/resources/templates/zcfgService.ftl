[${id}]
Title = ${title}
Abstract = ${description}
processVersion = ${version}
storeSupported = ${storeSupported?c}
statusSupported = ${statusSupported?c}
serviceType = ${serviceType}
serviceProvider = ${serviceProvider}

<#if dataInputs??>
<#list dataInputs>
<DataInputs>
    <#items as param>
    [${param.id}]
    Title = ${param.title}
    Abstract = ${param.description}
    minOccurs = ${param.minOccurs}
    maxOccurs = ${param.maxOccurs}
    <#if param.data == "LITERAL">
        <#assign nodeTag = "LiteralData">
    <#elseif param.data == "COMPLEX">
        <#assign nodeTag = "ComplexData">
    <#elseif param.data == "BOUNDING_BOX">
        <#assign nodeTag = "BoundingBoxData">
    </#if>
    <${nodeTag}>
        <Default>
            <#list param.defaultAttrs as key, value>
            ${key} = ${value}
            </#list>
        </Default>
        <#list param.supportedAttrs as supported>
        <Supported>
            <#list supported as key, value>
            ${key} = ${value}
            </#list>
        </Supported>
        </#list>
    </${nodeTag}>
    </#items>
</DataInputs>
</#list>
</#if>

<#if dataOutputs??>
<#list dataOutputs>
<DataOutputs>
    <#items as param>
    [${param.id}]
    Title = ${param.title}
    Abstract = ${param.description}
    <#if param.data == "LITERAL">
        <#assign nodeTag = "LiteralData">
    <#elseif param.data == "COMPLEX">
        <#assign nodeTag = "ComplexData">
    <#elseif param.data == "BOUNDING_BOX">
        <#assign nodeTag = "BoundingBoxData">
    </#if>
    <${nodeTag}>
        <Default>
            <#list param.defaultAttrs as key, value>
            ${key} = ${value}
            </#list>
        </Default>
        <#list param.supportedAttrs as supported>
            <Supported>
                <#list supported as key, value>
                ${key} = ${value}
                </#list>
            </Supported>
        </#list>
    </${nodeTag}>
    </#items>
</DataOutputs>
</#list>
</#if>
