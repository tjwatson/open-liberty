rm -r substrate_build/temp/servlet
mkdir -p substrate_build/temp/servlet
rm -r ../build.image/wlp/usr/servers/substrateServlet
mkdir -p ../build.image/wlp/usr/servers/substrateServlet
cp substrate_servlet/server.xml ../build.image/wlp/usr/servers/substrateServlet
cp substrate_servlet/bootstrap.properties ../build.image/wlp/usr/servers/substrateServlet
../build.image/wlp/bin/server package substrateServlet --archive=`pwd`/substrate_build/temp/substrateServlet.zip --include=minify
unzip -q substrate_build/temp/substrateServlet.zip -d substrate_build/temp/servlet

mkdir substrate_build/temp/servlet/wlp/substrate_lib

mv substrate_build/temp/servlet/wlp/lib/*.jar substrate_build/temp/servlet/wlp/substrate_lib

rm -r substrate_build/temp/servlet/wlp/lib/extract

rm substrate_build/temp/servlet/wlp/substrate_lib/bootstrap-agent.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.json4j_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.config.schemagen_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.config.server.schemagen_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.crypto.certificateutil_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.crypto.ltpakeyutil_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.install.map_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.install_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.kernel.boot.archive_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.kernel.cmdline_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.logging.hpel.osgi_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.logging.hpel_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.org.glassfish.json.1.0_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.product.utility_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.repository.liberty_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.repository.parsers_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.repository.resolver_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.repository_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.security.utility_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/com.ibm.ws.webserver.plugin.utility_*.jar
rm substrate_build/temp/servlet/wlp/substrate_lib/ws-launch.jar

cp substrate_build/temp/servlet/wlp/dev/api/spec/com.ibm.websphere.javaee.servlet.3.1_*.jar substrate_build/temp/servlet/wlp/substrate_lib
cp substrate_build/temp/servlet/wlp/dev/api/spec/com.ibm.websphere.javaee.el.3.0_*.jar substrate_build/temp/servlet/wlp/substrate_lib
cp substrate_build/temp/servlet/wlp/dev/api/spec/com.ibm.websphere.javaee.jsp.2.3_*.jar substrate_build/temp/servlet/wlp/substrate_lib
cp substrate_build/temp/servlet/wlp/dev/api/spec/com.ibm.websphere.javaee.annotation.1.1_*.jar substrate_build/temp/servlet/wlp/substrate_lib
cp substrate_build/temp/servlet/wlp/dev/api/spec/com.ibm.websphere.javaee.activity.1.0_*.jar substrate_build/temp/servlet/wlp/substrate_lib
cp substrate_build/temp/servlet/wlp/dev/api/spec/com.ibm.websphere.javaee.jstl.1.2_*.jar substrate_build/temp/servlet/wlp/substrate_lib

rm -r substrate_build/temp/servlet/wlp/dev

rm -r substrate_build/temp/servlet/wlp/usr/servers/*
mkdir substrate_build/temp/servlet/wlp/usr/servers/defaultServer

cp substrate_servlet/server.xml substrate_build/temp/servlet/wlp/usr/servers/defaultServer/
cp substrate_servlet/bootstrap.properties substrate_build/temp/servlet/wlp/usr/servers/defaultServer/

cp ../cnf/staging/repository/com/ibm/ws/org/atomos/atomos.framework/0.0.1/atomos.framework-0.0.1.jar substrate_build/temp/servlet/wlp/substrate_lib
cp ../build.image/wlp/lib/com.ibm.ws.kernel.atomos_*.jar substrate_build/temp/servlet/wlp/substrate_lib

$JAVA_HOME/bin/native-image -cp \
substrate_servlet:\
"substrate_build/temp/servlet/wlp/substrate_lib/*"\
 -H:Name=substrate_build/temp/servlet/wlp/liberty