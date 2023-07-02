#!/bin/bash

#
# Copyright (c) 2009 - 2022, DHBW Mannheim - TIGERs Mannheim
#

INSTALL_DIR=build/install/sumatra
ARGS="$@"

if [[ -d "${INSTALL_DIR}" ]]; then
  ${INSTALL_DIR}/bin/sumatra ${ARGS}
else
  echo "Sumatra not installed, running with Gradle"
  if [[ -n "${ARGS}" ]]; then
    ./gradlew :run --args="${ARGS}"
  else
    ./gradlew :run
  fi
fi
