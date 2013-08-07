<#include "./../header.ftl"> 
<#import "/spring.ftl" as spring />

<div id="configuration">

	<h1>Service Proxy Configuration</h1>
	<span style="color:red "><#if RequestParameters.Error??>${RequestParameters.Error}</#if></span>

	<#if (networkServers?? && networkServers?size!=0) || (UDSServers?? && UDSServers?size!=0) >
		<h2>Servers</h2>
		<table id = "sp-conf-table-servers" class="bordered-table">
			<tr>
				<th>ServerType</th>
				<th>Endpoint</th>
				<th>Registered Channel Handlers</th>
			</tr>
			<#if networkServers?? && networkServers?size!=0>
				<#list networkServers as nServer>
					<tr>
						<td>${nServer.getServerType()}</td>
						<td>${nServer.getServerEndpoint()}</td>
						<td><#list nServer.pipelineFactory.channelHandlersMap?keys as chkey> ${chkey}, </#list></td>
					</tr>
				</#list>
			</#if>
		</table>
	</#if>
	
	
	<#if handlers?? && handlers?size!=0>
		<h2>Handlers</h2>
		<table id = "sp-conf-table" class="bordered-table">
			<tr>
				<th> Handler Name </th>
				<th> Description </th>
			</tr>
			<#list handlers?keys as handlerName>
				<tr>
					<#assign job_url><@spring.url relativeUrl="/viewConfig/handler/${handlerName}"/></#assign>
					<td><a href="${job_url}">${handlerName}</a></td>
					<td>${handlers[handlerName]}</td>
				</tr>
			</#list>
		</table>
	</#if>

</div>

<#include "./../footer.ftl"> 

