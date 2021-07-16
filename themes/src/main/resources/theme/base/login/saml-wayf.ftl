<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${provider} - SAML Aggregate WAYF
    <#elseif section = "form">

        <#if isLogin>

        <h3>LOGIN</h3>

            <form id="kc-form-saml-wayf" action="${actionUrl}" method="GET" onsubmit="return storeEntityId(this);">
                <div class="${properties.kcFormGroupClass!}">
                    <label for="idplist" class="${properties.kcLabelClass!}">${msg("saml-wayf-select-label")}</label>
                    <select id="idplist" name="entity_id" class="${properties.kcSelectClass!}">
                    <#list descriptors as d>
                       <option value="${d.entityId}">${d.entityId}</option>
                    </#list>
                    </select>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                   <input type="hidden" name="provider" value="${provider}" />
                   <input type="hidden" name="client_id" value="${clientId}" />
                   <input type="hidden" name="state" value="${state}" />
                   <input type="hidden" name="kc_idp_hint" value="${provider}" />
                   <input type="hidden" name="response_type" value="${responseType}" />
                   <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="wayf-login" id="kc-wayf-login" type="submit" value="${msg("saml-wayf-dologin")}"/>
                </div>
            </form>
        </#if>

        <#if isLinking>

        <h3>LINKING</h3>

                    <form id="kc-form-saml-wayf" action="${actionUrl}" method="GET" onsubmit="return storeEntityId(this);">
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="idplist" class="${properties.kcLabelClass!}">${msg("saml-wayf-select-label")}</label>
                            <select id="idplist" name="entity_id" class="${properties.kcSelectClass!}">
                            <#list descriptors as d>
                               <option value="${d.entityId}">${d.entityId}</option>
                            </#list>
                            </select>
                        </div>

                        <div class="${properties.kcFormGroupClass!}">
                           <input type="hidden" name="provider" value="${provider}" />
                           <input type="hidden" name="nonce" value="${nonce}" />
                           <input type="hidden" name="hash" value="${hash}" />
                           <input type="hidden" name="client_id" value="${client_id}" />
                           <input type="hidden" name="redirect_uri" value="${redirect_uri}" />
                           <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="wayf-login" id="kc-wayf-login" type="submit" value="${msg("saml-wayf-dologin")}"/>
                        </div>
                    </form>
                </#if>

    </#if>

<script type="text/javascript">

  function setCookie(name, value) {
    var today = new Date();
    var expiry = new Date(today.getTime() + 1800 * 1000); // plus 30 minutes
    document.cookie=name + "=" + escape(value) + "; path=/; expires=" + expiry.toGMTString();
  }

  function storeEntityId(form) {
    <#if isLinking>
    setCookie("SAML_AGGREGATE_WAYF_LINKING_" + form.provider.value, form.entity_id.value);
    </#if>
    <#if isLogin>
    setCookie("SAML_AGGREGATE_WAYF_LOGIN_" + form.provider.value, form.entity_id.value);
    </#if>
    return true;
  }

</script>
</@layout.registrationLayout>