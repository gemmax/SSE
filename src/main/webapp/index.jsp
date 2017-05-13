<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ page import="java.util.*,java.io.*" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>

        <title>Gemmax Search</title>
        <link rel="stylesheet" type="text/css" href="searchstyle.css">
    </head>

    <body>
        <p align="center"><img src="logo.png" /></p>
        <form id="form" action="search.jsp" name="search" method="get">
            <span class="quickdelete-wrap s_ipt_wr">
                <input id="kw" name="wd" class="s_ipt" maxlength="255" autocomplete="off" width="100%">
            </span>
            <span class="s_btn_wr">
                <input type="submit" id="su" value="Search" class="s_btn">
            </span>
        </form>
    </body>
</html>