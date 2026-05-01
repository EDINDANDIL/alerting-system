#!/bin/sh
set -eu

echo "Creating Kafka topics..."

KAFKA="/opt/kafka/bin/kafka-topics.sh"
BOOTSTRAP="kafka:9092"

$KAFKA --create --if-not-exists --bootstrap-server "$BOOTSTRAP" --topic trades-topic --partitions 32 --replication-factor 1 --config cleanup.policy=delete --config retention.ms=1000 --config segment.ms=1000 --config segment.bytes=1048576
$KAFKA --create --if-not-exists --bootstrap-server "$BOOTSTRAP" --topic filter-topic --partitions 8 --replication-factor 1 --config cleanup.policy=delete --config retention.ms=604800000 --config segment.ms=3600000 --config segment.bytes=104857600
$KAFKA --create --if-not-exists --bootstrap-server "$BOOTSTRAP" --topic command-topic --partitions 8 --replication-factor 1 --config cleanup.policy=delete --config retention.ms=604800000 --config segment.ms=3600000 --config segment.bytes=104857600
$KAFKA --create --if-not-exists --bootstrap-server "$BOOTSTRAP" --topic alert-topic --partitions 8 --replication-factor 1 --config cleanup.policy=delete --config retention.ms=5000 --config segment.ms=1000 --config segment.bytes=1048576

$KAFKA --list --bootstrap-server "$BOOTSTRAP"

echo "Kafka topics ready."
