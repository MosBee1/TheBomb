cd ~/TheBomb

if [ ! -s fastlane/metadata/android/en-US/changelogs/2.txt ]; then
  cat > fastlane/metadata/android/en-US/changelogs/2.txt <<'EOF'
1.1: Floating popup redesign — compact card over any app, tap outside to
close. Popup and notification no longer show together. New Permissions
dashboard in Settings with live status and one-tap fixes.
EOF
fi

FAIL=0
check() {
  if grep -q "$2" "$1" 2>/dev/null; then echo "OK   $3"; else echo "FAIL $3"; FAIL=1; fi
}

check app/build.gradle.kts 'versionCode = 2' "versionCode bumped"
check app/build.gradle.kts 'versionName = "1.1"' "versionName 1.1"
check app/src/main/res/values/strings_permissions.xml 'perm_row_images' "permission strings"
check app/src/main/java/io/github/mosbee1/thebomb/ui/settings/SettingsScreen.kt 'PermissionDashboard(mainViewModel)' "dashboard wired into Settings"
check app/src/main/java/io/github/mosbee1/thebomb/ui/settings/PermissionDashboard.kt 'fun PermissionDashboard' "dashboard file exists"
check fastlane/metadata/android/en-US/full_description.txt 'PERMISSIONS, IN PLAIN SIGHT' "description has new section"
check fastlane/metadata/android/en-US/full_description.txt 'MATERIAL YOU' "description complete"
[ -s fastlane/metadata/android/en-US/changelogs/2.txt ] && echo "OK   changelog present" || { echo "FAIL changelog missing"; FAIL=1; }
if grep -q "changelogs" fastlane/metadata/android/en-US/full_description.txt; then
  echo "FAIL description polluted"; FAIL=1
else
  echo "OK   description clean"
fi

if [ "$FAIL" = "1" ]; then
  echo "=== GATE FAILED - DO NOT PUSH - PASTE THIS OUTPUT IN CHAT ==="
else
  git pull origin main
  git add -A
  if git diff --cached --quiet; then
    echo "nothing new to commit - pushing anyway to sync"
  else
    git commit -m "v1.1: Permissions dashboard; official store description; version bump"
  fi
  git push
  echo "=== ALL CHECKS PASSED - PUSHED - WATCH ACTIONS ==="
fi
