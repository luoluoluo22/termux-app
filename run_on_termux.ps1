param (
    [Parameter(Mandatory=$true)]
    [string]$Command
)

# Check ADB devices
$devices = adb devices
if ($devices.Length -lt 2) {
    Write-Error "No connected ADB devices found!"
    exit 1
}

# 1. Create temporary local script
$localScriptPath = Join-Path $env:TEMP "termux_runner_temp.sh"
$template = @'
#!/data/data/com.termux/files/usr/bin/bash
exec > >(tee /data/data/com.termux/files/home/runner.log) 2>&1
{COMMAND_TO_EXECUTE}
'@

$scriptContent = $template -replace '\{COMMAND_TO_EXECUTE\}', $Command

# 2. Push script to Termux home directory
$unixContent = $scriptContent -replace "`r`n", "`n"
[System.IO.File]::WriteAllText($localScriptPath, $unixContent, [System.Text.Encoding]::UTF8)

Get-Content -Raw $localScriptPath | adb shell run-as com.termux "sh -c 'cat > /data/data/com.termux/files/home/runner.sh'"
adb shell run-as com.termux chmod 700 /data/data/com.termux/files/home/runner.sh

# Clean up temp file
Remove-Item $localScriptPath -ErrorAction SilentlyContinue | Out-Null

# 3. Check and wake screen if needed
$powerState = adb shell dumpsys power
if ($powerState -match "mWakefulness=Dozing" -or $powerState -match "mWakefulness=Asleep" -or $powerState -match "mWakefulness=Dreaming") {
    Write-Host "Screen is asleep, waking up and unlocking..."
    adb shell input keyevent KEYCODE_WAKEUP
    Start-Sleep -Milliseconds 800
    adb shell input swipe 500 1500 500 500 300
    Start-Sleep -Milliseconds 800
}

# 4. Bring Termux to foreground
adb shell am start -n com.termux/.app.TermuxActivity | Out-Null
Start-Sleep -Milliseconds 800

# 5. Simulate input to run script
adb shell input text "./runner.sh"
adb shell input keyevent 66

Write-Host "Command sent to Termux foreground. Waiting for completion..."
Start-Sleep -Seconds 3

# 6. Capture screen and pull to artifacts
$artifactDir = "C:\Users\Administrator\.gemini\antigravity\brain\c0c22810-0a84-41b8-bd7a-a7ab182f0438"
$screenshotPath = Join-Path $artifactDir "screenshot.png"
adb shell screencap -p /sdcard/screencap.png
adb pull /sdcard/screencap.png $screenshotPath | Out-Null

# 7. Print terminal log output
$log = adb shell run-as com.termux cat /data/data/com.termux/files/home/runner.log
Write-Host "`n=== TERMUX RUN RESULT ===" -ForegroundColor Green
Write-Host $log
Write-Host "==========================`n" -ForegroundColor Green
Write-Host "Screenshot saved to: $screenshotPath"
