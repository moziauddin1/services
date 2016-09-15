<%@ page import="au.org.biodiversity.nsl.ConfigService; org.apache.shiro.SecurityUtils" %>
<div class="container">
  <h1>${grailsApplication.config.shard.product} <g:if test="${params?.product}">- ${params.product}</g:if></h1>
</div>

<div class="navbar navbar-inverse" role="navigation">
  <div class="">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <a class="navbar-brand" href="${createLink(uri: '/')}">
        NSL
      </a>
    </div>

    <div class="collapse navbar-collapse">
      <ul class="nav navbar-nav">
        <li class="${params.controller == 'dashboard' ? 'active' : ''}">
          <a class="dashboard" href="${createLink(controller: 'dashboard', action: 'index')}"><i
              class="fa fa-bar-chart-o"></i> Dashboard</a>
        </li>
        <g:if test="${SecurityUtils.subject?.principal}">
          <li class="${params.controller == 'search' ? 'active' : ''}">
            <a class="search" href="${createLink(controller: 'search', action: 'search')}"><i
                class="fa fa-search"></i> Search</a>
          </li>
        </g:if>
        <li class="${params.product == ConfigService.nameTreeName ? 'active' : ''}">
          <a class="search"
             href="${createLink(controller: 'search', action: 'search', params: [product: ConfigService.nameTreeName])}"><i
              class="fa fa-search"></i> Names (<st:nameTree/>)</a>
        </li>
        <li class="${params.product == ConfigService.classificationTreeName ? 'active' : ''}">
          <a class="search"
             href="${createLink(controller: 'search', action: 'search', params: [product: ConfigService.classificationTreeName])}"><i
              class="fa fa-search"></i> Taxonomy (<st:primaryClassification/>)</a>
        </li>
      </ul>

      <ul class="nav navbar-nav navbar-right">

        <li>
          <st:documentationLink/>
        </li>

        <shiro:isLoggedIn>
          <shiro:hasRole name="admin">
            <li>
              <a class="home" href="${createLink(controller: 'admin', action: 'index')}">
                <i class="fa fa-gears"></i> admin
              </a>
            </li>
          </shiro:hasRole>
          <li class="active">
            <a class="logout" href="${createLink(controller: 'auth', action: 'signOut')}">
              <i class="fa fa-user${shiro.hasRole(name: 'admin') {
                '-plus'
              }}"></i> <span>${SecurityUtils.subject?.principal}</span>
              -
              <i class="fa fa-power-off"></i> Logout
            </a>
          </li>
        </shiro:isLoggedIn>
        <shiro:isNotLoggedIn>
          <li class="dropdown">
            <a id="dLabel" data-target="#" href="${createLink(controller: 'auth', action: 'login')}"
               data-toggle="dropdown" aria-haspopup="true" role="button" aria-expanded="false">
              <i class="fa fa-power-off"></i> Login
              <span class="caret"></span>
            </a>
            <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
              <li>
                <g:form controller="auth" action="signIn">
                  <input type="hidden" name="targetUri" value="${request.forwardURI - request.contextPath}"/>
                  <table>
                    <tbody>
                    <tr>
                      <td>Username:</td>
                      <td><input type="text" name="username" value=""/></td>
                    </tr>
                    <tr>
                      <td>Password:</td>
                      <td><input type="password" name="password" value=""/></td>
                    </tr>
                    <tr>
                      <td>Remember me?:</td>
                      <td><g:checkBox name="rememberMe" value=""/></td>
                    </tr>
                    <tr>
                      <td/>
                      <td><input class="btn btn-default" type="submit" value="Login"/></td>
                    </tr>
                    </tbody>
                  </table>
                </g:form>
              </li>
            </ul>
          </li>
        </shiro:isNotLoggedIn>
      </ul>

    </div><!--/.nav-collapse -->
  </div>
</div>
<g:if test="${flash.message}">
  <div class="alert alert-warning" role="alert">
    <span class="fa fa-warning" aria-hidden="true"></span>&nbsp;${flash.message}</div>
</g:if>
