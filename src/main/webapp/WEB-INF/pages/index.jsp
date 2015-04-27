<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sf" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<c:set var="cp" value="${pageContext.request.servletContext.contextPath}" scope="request"/>

<!DOCTYPE html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Algu kalkulators</title>
    <link rel="stylesheet" type="text/css" href="${cp}/resources/css/site.css"/>
    <script src="${cp}/resources/js/js.js"></script>
</head>
<form method="post" action="/calculate">
    <label for="bruto">Bruto alga:</label>
    <input type="number" id="bruto" name="bruto" pattern="[0-9]" min="360" step="0.01"/>
    <br>
    <br>
    <label for="checbox">Pievienot atvieglojumus?</label>
    <input type="checkbox" id="checbox" name="checbox" onclick="checkInput(this);"/>
    <p id="newInput"></p>
    <input type="submit" name="action" value="Aprēķināt un konvertēt"/>

    <table>
        <tr>
            <td><h3>Neto alga: ${neto}</h3></td>
        </tr>
        <tr>
            <td>
                <form:select name="valuta" path="currenciesList">
                    <c:forEach items="${currenciesList}" var="currency">
                        <option value="${currency.key}">${currency.key}</option>
                    </c:forEach>
                </form:select>
            </td>
            <td>Konvertētā valūta:</td>
            <td>${convertedValue}</td>
        </tr>
    </table>
</form>

<br>
<table>
    <tr>
        <td>VSAI (23.59%):</td>
        <td>${socIem}</td>
    </tr>
    <tr>
        <td>Uzņēmējdarbības riska valsts nodeva:</td>
        <td>${risks}</td>
    </tr>
    <tr>
        <td>Darba devēja izmaksas kopā:</td>
        <td>${kopa}</td>
    </tr>
</table>

<script type="text/javascript">
    function checkInput(cbox) {
        if (cbox.checked) {
            var input = document.createElement("input");
            input.type = "number";
            input.pattern = "[0-9]";
            input.min = "1";
            input.name = "atvieglojumi";
            var div = document.createElement("div");
            div.id = cbox.name;
            div.innerHTML = "Personu skaits apgādībā: ";
            div.appendChild(input);
            document.getElementById("newInput").appendChild(div);
        } else {
            document.getElementById(cbox.name).remove();
        }
    }
</script>
</body>
</html>
