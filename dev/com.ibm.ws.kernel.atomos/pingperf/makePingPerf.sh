#!/bin/bash
set -xe
rm -rf build/
mkdir -p ./build/temp
rm -rf ../../build.image/wlp/usr/servers/defaultServer/
mkdir ../../build.image/wlp/usr/servers/defaultServer/

# copy over server config and feature definition for to prepare for server package
for x in `find wlp -type f` 
do  
  rm -f ../../build.image/$x
  cp $x ../../build.image/$x
done
../../build.image/wlp/bin/server package defaultServer --archive=`pwd`/build/temp/minifiedPingPerfWlp.zip --include=minify

unzip -q ./build/temp/minifiedPingPerfWlp.zip -d ./build/
minified_wlp=`pwd`/build/wlp
rm -r $minified_wlp/lib/extract
cpl=$minified_wlp/classpath_lib
rm -rf $cpl
mkdir $cpl

set +x
echo populating $cpl ...

echo Copy over files from bundles_needed.txt
for b in `cat ./bundles_needed.txt`
do
    #echo cp $minified_wlp/$b $cpl 
    cp $minified_wlp/$b $cpl 
done

echo copy over bundles of type boot.jar
for b in `cat ./bootJars_needed.txt`
do
    cp $minified_wlp/$b $cpl 
done
set +x

cp ../../cnf/staging/repository/com/ibm/ws/org/atomos/atomos.framework/0.0.1/atomos.framework-0.0.1.jar $cpl
cp ../../build.image/wlp/lib/com.ibm.ws.kernel.atomos_*.jar $cpl

rm -f build/wlp/lib/*.jar
rm -rf build/wlp/templates

#export GRAALVM_HOME=/Users/sbratton/Applications/graalvm-ce-java11-19.3.1/Contents/Home
#$GRAALVM_HOME/bin/native-image -cp \
# $minified_wlp/classpath_lib:\
#  -H:Name=subst.pp.build/temp/pingperf/wlp/liberty


cat > ./build/launchPingPerf.sh <<- EOF
	debug=""
	while test \$# -gt 0
	do
	    case "\$1" in
	        debug) 
	            debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000"
	            ;;
	        clean)
	            rm -fr $minified_wlp/usr/servers/defaultServer/workarea
	            echo rm -fr $minified_wlp/usr/servers/defaultServer/workarea
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

echo -e "Launch server with ./build/launchPingPerf.sh\n  URLs: http://localhost:9080/ping/ping/{greeting|simple}"

