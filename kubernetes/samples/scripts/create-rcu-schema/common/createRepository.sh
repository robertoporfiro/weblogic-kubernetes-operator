# Copyright 2018, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#

. /u01/oracle/wlserver/server/bin/setWLSEnv.sh

echo "Check if the DB Service is Ready to accept Connection ?"
connectString=${1:-oracle-db.default.svc.cluster.local:1521/devpdb.k8s}
schemaPrefix=${2:-domain1}
echo "DB Connection URL [$connectString] and schemaPrefix is [${schemaPrefix}]"

max=20
counter=0
while [ $counter -le ${max} ]
do
 #java utils.dbping ORACLE_THIN "sys as sysdba" Oradoc_db1 ${connectString}
 java utils.dbping ORACLE_THIN scott tiger ${connectString} > dbping.err 2>&1
 [[ $? == 0 ]] && break;
 ((counter++))
 echo "[$counter/${max}] Retrying the DB Connection ..."
 sleep 10
done

if [ $counter -gt ${max} ]; then
 echo "[ERRORR] Oracle DB Service is not ready after [${max}] iterations ..."
 exit -1
else
 java utils.dbping ORACLE_THIN scott tiger ${connectString}
fi

/u01/oracle/oracle_common/bin/rcu -silent -createRepository \
 -databaseType ORACLE -connectString ${connectString} \
 -dbUser sys  -dbRole sysdba -useSamePasswordForAllSchemaUsers true \
 -selectDependentsForComponents true \
 -schemaPrefix ${schemaPrefix} \
 -component MDS -component IAU -component IAU_APPEND -component IAU_VIEWER \
 -component OPSS  -component WLS -component STB < /u01/oracle/pwd.txt