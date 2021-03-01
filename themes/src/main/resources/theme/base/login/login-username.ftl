<#import "template.ftl" as layout>
<#noautoesc>
    <script>
        var identityProvidersSummary = JSON.parse("${identityProvidersSummary?js_string}");
    </script>
</#noautoesc>
	<script>
	
		var aClass = '${properties.kcFormSocialAccountListButtonClass!}'.split(" ");
		
		function buildFiltered(value) {
			
			if(!Array.isArray(identityProvidersSummary)) return;
			
			var listElem = document.getElementById('kc-providers-list');
			if(listElem==null)
				return;
			listElem.textContent = "";
			for(var i=0; i<identityProvidersSummary.length; i++ ){
				var idp = identityProvidersSummary[i];
				
				if(idp.displayName.toLowerCase().lastIndexOf(value.toLowerCase())>=0){
					var span = document.createElement('span');
					span.textContent = idp.displayName;
					span.classList.add("kc-social-provider-name")
					var a = document.createElement('a');
					a.href = idp.loginUrl;
					a.id = "social-" + idp.alias;
					for (var j = 0; j < aClass.length; j++) {
					   a.classList.add(aClass[j]);
					}
					
					//var li = document.createElement('li');
					
					a.appendChild(span);
					//li.appendChild(a);
					//listElem.appendChild(li);
					listElem.appendChild(a);
				}
			}
		};
		
		window.onload = function() {
		  buildFiltered("");
		};
		
    </script>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}"
                          method="post">
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="username"
                                   class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                            <#if usernameEditDisabled??>
                                <input tabindex="1" id="username"
                                       aria-invalid="<#if message?has_content && message.type = 'error'>true</#if>"
                                       class="${properties.kcInputClass!}" name="username"
                                       value="${(login.username!'')}"
                                       type="text" disabled/>
                            <#else>
                                <input tabindex="1" id="username"
                                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                                       class="${properties.kcInputClass!}" name="username"
                                       value="${(login.username!'')}"
                                       type="text" autofocus autocomplete="off"/>
                            </#if>

                            <#if messagesPerField.existsError('username')>
                                <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                </span>
                            </#if>
                        </div>

                        <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                            <div id="kc-form-options">
                                <#if realm.rememberMe && !usernameEditDisabled??>
                                    <div class="checkbox">
                                        <label>
                                            <#if login.rememberMe??>
                                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"
                                                       checked> ${msg("rememberMe")}
                                            <#else>
                                                <input tabindex="3" id="rememberMe" name="rememberMe"
                                                       type="checkbox"> ${msg("rememberMe")}
                                            </#if>
                                        </label>
                                    </div>
                                </#if>
                            </div>
                        </div>

                        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <input tabindex="4"
                                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                   name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                        </div>
                    </form>
                </#if>
        </div>

            <#if realm.password && social.providers??>
                  <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                  <hr/>
                  <h4>${msg("identity-provider-login-label")}</h4>
                  <#if social.providers?size gt 8>
            		<input id="kc-providers-filter" type="text" placeholder="Filter..." oninput="buildFiltered(this.value)">
            		<ul id="kc-providers-list" class="${properties.kcFormSocialAccountListClass!} login-pf-list-scrollable"></ul>
            	 <#else>
                	 <ul class="${properties.kcFormSocialAccountListClass!} ">
                     <#list social.providers as p>
                        <a id="social-${p.alias}" class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                                type="button" href="${p.loginUrl}">
                            <#if p.iconClasses?has_content>
                                <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                            <#else>
                                <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                            </#if>
                        </a>
                     </#list>
                     </ul>
                 </#if>
                 </div>
            </#if>

    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
