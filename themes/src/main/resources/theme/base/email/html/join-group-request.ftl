<html>
<body>
${kcSanitize(msg("joinGroupRequestBodyHtml",userRequest.getUsername(),userRequest.getFirstName(),userRequest.getLastName(),url))?no_esc}
</body>
</html>