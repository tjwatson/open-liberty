rm -r substrate_build/temp/gogo
mkdir -p substrate_build/temp/gogo
rm -r ../build.image/wlp/usr/servers/substrateGogo
mkdir -p ../build.image/wlp/usr/servers/substrateGogo
cp substrate_gogo/server.xml ../build.image/wlp/usr/servers/substrateGogo
cp substrate_gogo/bootstrap.properties ../build.image/wlp/usr/servers/substrateGogo
../build.image/wlp/bin/server package substrateGogo --archive=`pwd`/substrate_build/temp/substrateGogo.zip --include=minify
unzip -q substrate_build/temp/substrateGogo.zip -d substrate_build/temp/gogo

mkdir substrate_build/temp/gogo/wlp/substrate_lib

mv substrate_build/temp/gogo/wlp/lib/*.jar substrate_build/temp/gogo/wlp/substrate_lib

rm -r substrate_build/temp/gogo/wlp/lib/extract

rm substrate_build/temp/gogo/wlp/substrate_lib/bootstrap-agent.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.json4j_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.config.schemagen_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.config.server.schemagen_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.crypto.certificateutil_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.crypto.ltpakeyutil_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.install.map_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.install_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.kernel.boot.archive_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.kernel.cmdline_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.logging.hpel.osgi_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.logging.hpel_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.org.glassfish.json.1.0_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.product.utility_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.repository.liberty_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.repository.parsers_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.repository.resolver_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.repository_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/com.ibm.ws.security.utility_*.jar
rm substrate_build/temp/gogo/wlp/substrate_lib/ws-launch.jar

rm -r substrate_build/temp/gogo/wlp/dev

rm -r substrate_build/temp/gogo/wlp/usr/servers/*
mkdir substrate_build/temp/gogo/wlp/usr/servers/defaultServer

cp substrate_gogo/server.xml substrate_build/temp/gogo/wlp/usr/servers/defaultServer/
cp substrate_gogo/bootstrap.properties substrate_build/temp/gogo/wlp/usr/servers/defaultServer/

cp ../cnf/staging/repository/com/ibm/ws/org/atomos/atomos.framework/0.0.1/atomos.framework-0.0.1.jar substrate_build/temp/gogo/wlp/substrate_lib
cp ../build.image/wlp/lib/com.ibm.ws.kernel.atomos_*.jar substrate_build/temp/gogo/wlp/substrate_lib

$JAVA_HOME/bin/native-image -cp \
substrate_gogo:\
"substrate_build/temp/gogo/wlp/substrate_lib/*"\
 -H:Name=substrate_build/temp/gogo/wlp/liberty

mv substrate_build/temp/gogo/wlp/substrate_lib substrate_build/temp/gogo/wlp/classpath_lib
