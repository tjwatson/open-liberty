#!/bin/bash
set -xe
rm -rf build/
mkdir -p ./build/temp
dev=`cd ../.. ; pwd`
rm -rf $dev/build.image/wlp/usr/servers/pingperfServer
unzip -d $dev/build.image/wlp/usr/servers/ ./pingperf-server-config.zip 
$dev/build.image/wlp/bin/server package pingPerfServer --archive=`pwd`/build/temp/minifiedPingPerfWlp.zip --include=minify
unzip -q ./build/temp/minifiedPingPerfWlp.zip -d ./build/

minified_wlp=`pwd`/build/wlp
rm -r $minified_wlp/lib/extract
cpl=$minified_wlp/classpath_lib
rm -rf $cpl
mkdir $cpl

set +x
echo populating $cpl ...

echo Copy over files specified in minified.bundles.lb.tx
for b in `cat ./bundles_needed.txt`
do
    #echo cp $minified_wlp/$b $cpl 
    cp $minified_wlp/$b $cpl 
done

echo copy over bundles of type="boot.jar"
for b in `cat ./bootJars_needed.txt`
do
    cp $minified_wlp/$b $cpl 
done
set +x

rm -r $minified_wlp/usr/servers/*
pp_server=$minified_wlp/usr/servers/defaultServer
mkdir $pp_server

unzip -d $minified_wlp/usr/servers ./pingperf-server-config.zip \
 pingPerfServer/server.xml pingPerfServer/jvm.options pingPerfServer/bootstrap.properties pingPerfServer/apps/\*

rm -fr $minified_wlp/usr/servers/defaultServer
mv $minified_wlp/usr/servers/pingPerfServer $minified_wlp/usr/servers/defaultServer

cp $dev/cnf/staging/repository/com/ibm/ws/org/atomos/atomos.framework/0.0.1/atomos.framework-0.0.1.jar $cpl
cp $dev/build.image/wlp/lib/com.ibm.ws.kernel.atomos_*.jar $cpl

#$JAVA_HOME/bin/native-image -cp \
#substrate_servlet:\
#"subst.pp.build/temp/pingperf/wlp/substrate_lib/*"\
# -H:Name=subst.pp.build/temp/pingperf/wlp/liberty

cat > ./build/launchPingPerf.sh <<- EOF
	debug=""
	while test \$# -gt 0
	do
	    case "\$1" in
	        debug) 
	            debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000"
	            ;;
	        clean)
	            rm -fr $new_wlp/usr/servers/defaultServer/workarea
	            echo rm -fr $new_wlp/usr/servers/defaultServer/workarea
	            ;;
	    esac
	    shift
	done
	
	set -xe
	cd $minified_wlp
	java \$debug -cp "classpath_lib/*" com.ibm.ws.kernel.atomos.Liberty
EOF
chmod a+x ./build/launchPingPerf.sh
echo -e "\n  COMPLETED\n"

echo -e "Launch server with ./build/launchPingPerf.sh\n  URLs: http://localhost:9080/pingperf/ping/{greeting|simple}"

