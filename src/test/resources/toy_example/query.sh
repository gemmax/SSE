SCRIPTPATH=$( cd $(dirname $0) ; pwd -P )
java -Xmx200M -cp $SCRIPTPATH/../../../../target/classes querier.Query Gamma $1
