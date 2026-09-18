#!/bin/sh
set -e
git config core.hooksPath scripts/hooks
chmod +x scripts/hooks/*
echo "Hooks installés (core.hooksPath = scripts/hooks)"
