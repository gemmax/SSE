SCRIPTPATH=$( cd $(dirname $0) ; pwd -P )
java -Xmx400M -cp $SCRIPTPATH/../../../../target/classes indexer.Index Gamma $1 $2
