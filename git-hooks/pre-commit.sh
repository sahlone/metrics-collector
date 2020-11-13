#!/bin/sh

echo "Running static analysis..."

# Format code using KtLint, then run Detekt and KtLint static analysis
# Run with fixes
./gradlew ktlintFormat detekt --daemon