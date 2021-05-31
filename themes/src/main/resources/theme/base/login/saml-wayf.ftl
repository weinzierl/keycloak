<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        SAML Aggregate WAYF
    <#elseif section = "form">

        <form id="kc-form-saml-wayf" action="${actionUrl}" method="GET">
            <div class="${properties.kcFormGroupClass!}">
                <label for="idplist" class="${properties.kcLabelClass!}">${msg("saml-wayf-select-label")}</label>
                <select id="idplist" name="entity_id" class="${properties.kcSelectClass!}">
                <#list descriptors as d>
                   <option value="${d.entityId}">${d.entityId}</option>
                </#list>
                </select>
            </div>

            <div class="${properties.kcFormGroupClass!}">
               <input type="hidden" name="client_id" value="${clientId}" />
               <input type="hidden" name="redirect_uri" value="${redirectUri}" />
               <input type="hidden" name="state" value="${state}" />
               <input type="hidden" name="kc_idp_hint" value="${provider}" />
               <input type="hidden" name="response_type" value="${responseType}" />
               <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="wayf-login" id="kc-wayf-login" type="submit" value="${msg("saml-wayf-dologin")}"/>
            </div>
        </form>

    </#if>
</@layout.registrationLayout>