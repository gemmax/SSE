<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<jsp:directive.page import="querier.Query" />
<%@page import="java.util.*, java.io.*" %>
<%@ page import="querier.Result" %>


<%
    String query = new String(request.getParameter("wd"));
    Result[] results = Query.query(query);
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
    <title>Search Result</title>
    <link rel="stylesheet" type="text/css" href="searchstyle.css">
    <link rel="stylesheet" type="text/css" href="listresult.css">
</head>

<body>
<div id="wrapper" style="margin-bottom: 32px">
<img src="logos.png" alt="" id="s_img"/>
<form id="form" action="search.jsp" name="search" method="get" style="display: inline-block">
    <span class="quickdelete-wrap s_ipt_wr">
        <input id="kw" name="wd" class="s_ipt" maxlength="255" autocomplete="off" value="<%=query%>" width="100%">
    </span>
    <span class="s_btn_wr">
        <input type="submit" id="su" value="Search" class="s_btn">
    </span>
</form>
</div>

<%
    if (results == null) {
%>
        <div class="g">
            <h3>no result found!</h3>
        </div>
<%
    } else {
        for(Result result : results) {
%>
                <div class="g">
                    <h3><a href=<%=result.getUrl()%>><%=result.getTitle()%></a></h3>
                    <div id="s">
                        <div>
                            <div id="f">
                            <cite><%=result.getUrl()%> </cite>
                            </div>
                            <span id="ci"><%=result.getContent(query.split("\\s+"))%></span>
                        </div>
                    </div>
                </div>
<%
        }
    }
%>

</body>
</html>