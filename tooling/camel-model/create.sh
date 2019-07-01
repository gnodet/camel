#!/bin/bash
#
#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

DIRNAME=$(pushd $(dirname "${0}") >/dev/null ; pwd ; popd >/dev/null)
CAMELDIR=$(pushd "${DIRNAME}/../../" >/dev/null ; pwd ; popd >/dev/null)

mkdir $DIRNAME/target 2>/dev/null
pushd $DIRNAME/target >/dev/null

echo "Building JSON catalog"
model=$(
	echo " { \"model\": { "
	ic=0
	for i in components dataformats languages models ; do
		if [ $ic -gt 0 ]
		then
			echo ", "
		fi
		pushd ${CAMELDIR}/catalog/camel-catalog/target/classes/org/apache/camel/catalog/$i >/dev/null
		echo "\"$i\": { "
		jc=0
		for j in *.json ; do
			if [ $jc -gt 0 ]
			then
				echo ", "
			fi
			n=${j%.*}
			echo "\"${n//+/-}\": "
			cat $j
			((jc++))
		done
		echo " } "
		popd >/dev/null
		((ic++))
	done
	echo " } } "
)
 echo $model > model.json

echo "Building XML model"
classpath=../src/staxon-gson-1.3.jar:../src/staxon-1.3.jar:../src/gson-2.8.5.jar:.
javac -cp $classpath ../src/JSON2XML.java -d .
cat model.json  | java -cp $classpath JSON2XML > model.xml

echo "Processing XML model"
xsltproc ../src/model.xslt model.xml > out.xml




