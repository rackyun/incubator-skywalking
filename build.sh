#!/usr/bin/env bash
git submodule init
git submodule update --remote
./mvnw clean package -Dmaven.test.skip=true "$@"
git rev-parse --short=10 HEAD > skywalking-agent/VERSION
cd skywalking-agent
tar -cf ../skywalking-agent.tar *