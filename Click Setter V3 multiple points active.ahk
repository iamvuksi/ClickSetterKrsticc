#Requires AutoHotkey v2.0
#SingleInstance Force
CoordMode "Mouse", "Screen"

; Click Setter by Krstic. Points and click settings can be saved as named presets.
; Set Point minimizes this window and captures the next screen click without
; passing that click through to the application underneath.

points := []
running := false
activeStopHotkey := ""
clickPeriod := 0
nextClickAt := 0
returnX := 0
returnY := 0
pointIndex := 1
currentPointPauseMs := 0
newPointPause := 0.5
waitingAfterMove := false
leaveCursorAtPoint := false

presetDir := A_ScriptDir "\presets"

; Small click-through countdown panel kept at the top-right of the primary screen.
countdownGui := Gui("+AlwaysOnTop -Caption +ToolWindow +E0x20", "Click countdown")
countdownGui.BackColor := "173B6C"
countdownGui.SetFont("s8 Bold cD9E7F5", "Segoe UI")
countdownGui.AddText("x8 y7 w164 h18 Center", "NEXT CLICK IN")
countdownGui.SetFont("s22 Bold cFFFFFF", "Segoe UI")
countdownLabel := countdownGui.AddText("x8 y25 w164 h39 Center", "--:--")
countdownGui.SetFont("s8 cD9E7F5", "Segoe UI")
countdownGui.AddText("x8 y66 w164 h16 Center", "CLICK SETTER  ·  KRSTIC")
ShowCountdownOverlay()

app := Gui("", "Click Setter | Fortnite Auto Play | By Krstic")
app.BackColor := "F3F6FA"
app.MarginX := 20
app.MarginY := 18
app.SetFont("s10 c243247", "Segoe UI")
app.SetFont("s19 Bold c173B6C", "Segoe UI")
app.AddText("x20 y18 w400 h32", "CLICK SETTER")
app.SetFont("s9 c637184", "Segoe UI")
app.AddText("x21 y53 w400 h22", "Fortnite Auto Play  ·  Designed by Krstic")

app.SetFont("s10 Bold c173B6C", "Segoe UI")
app.AddText("x20 y94 w400 h22", "CLICK POINTS")
app.SetFont("s9 c637184", "Segoe UI")
pointList := app.AddListView("x20 y122 w400 h170 Grid -Multi", ["#", "X", "Y", "Pause (sec)"])
pointList.ModifyCol(1, 38)
pointList.ModifyCol(2, 105)
pointList.ModifyCol(3, 105)
pointList.ModifyCol(4, 120)
pointList.OnEvent("ItemSelect", PointSelectionChanged)
addPointButton := app.AddButton("x20 y301 w120 h32", "Add Point")
addPointButton.OnEvent("Click", BeginPointCapture)
removePointButton := app.AddButton("x150 y301 w120 h32", "Remove Selected")
removePointButton.OnEvent("Click", RemoveSelectedPoint)
movePointButton := app.AddButton("x280 y301 w140 h32", "Move to Selected")
movePointButton.OnEvent("Click", MoveToSelectedPoint)
app.SetFont("s9 c637184", "Segoe UI")
app.AddText("x20 y348 w160 h24", "Pause after move (sec)")
pointPauseInput := app.AddEdit("x185 y341 w75 h28", "0.5")
applyPauseButton := app.AddButton("x270 y340 w150 h30", "Apply to Selected")
applyPauseButton.OnEvent("Click", ApplyPauseToSelected)

app.SetFont("s10 Bold c173B6C", "Segoe UI")
app.AddText("x20 y390 w400 h22", "CLICK SCHEDULE")
app.SetFont("s9 c637184", "Segoe UI")
app.AddText("x20 y418 w160 h24", "Interval between clicks (sec)")
intervalInput := app.AddEdit("x215 y411 w75 h28", "10")
app.AddText("x300 y418 w100 h24", "e.g. 10")
leaveCursorCheckbox := app.AddCheckbox("x20 y450 w400 h24", "Leave cursor at each point after clicking")
app.AddText("x20 y488 w160 h24", "Stop with key")
stopKeyInput := app.AddHotkey("x185 y481 w110 h28", "F8")

app.SetFont("s10 Bold c173B6C", "Segoe UI")
startButton := app.AddButton("x20 y519 w184 h36", "Start")
startButton.OnEvent("Click", StartClicks)
stopButton := app.AddButton("x216 y519 w184 h36 Disabled", "Stop")
stopButton.OnEvent("Click", StopClicks)

app.AddText("x20 y569 w400 h22", "PRESET")
app.SetFont("s9 c637184", "Segoe UI")
presetNameInput := app.AddEdit("x20 y598 w190 h28", "My preset")
savePresetButton := app.AddButton("x220 y596 w94 h32", "Save Preset")
savePresetButton.OnEvent("Click", SavePreset)
loadPresetButton := app.AddButton("x320 y596 w100 h32", "Load Preset")
loadPresetButton.OnEvent("Click", LoadPreset)

app.AddText("x20 y640 w400 h1 +0x10")
app.SetFont("s9 c243247", "Segoe UI")
statusLabel := app.AddText("x20 y652 w400 h54", "Ready  ·  Add points, set each point's pause, then start.")
app.SetFont("s8 c8793A3", "Segoe UI")
app.AddText("x20 y716 w400 h20 Center", "BY KRSTIC  |  AutoHotkey v2")
app.OnEvent("Close", (*) => ExitApp())
app.Show("w440 h748")

ShowCountdownOverlay() {
    global countdownGui
    x := A_ScreenWidth - 190
    if (x < 0)
        x := 0
    countdownGui.Show("NoActivate x" x " y24 w180 h88")
}

BeginPointCapture(*) {
    global app, countdownGui, statusLabel, pointPauseInput, newPointPause
    try {
        newPointPause := Float(Trim(pointPauseInput.Value))
    } catch {
        statusLabel.Text := "Enter a valid pause for the new point."
        return
    }
    if (newPointPause < 0 || newPointPause > 600000) {
        statusLabel.Text := "Point pause must be from 0 to 600,000 seconds."
        return
    }
    statusLabel.Text := "Click the exact location to add it. Press Esc to cancel."
    Hotkey "LButton", SavePoint, "On"
    Hotkey "Esc", CancelPointCapture, "On"
    countdownGui.Hide()
    WinMinimize "ahk_id " app.Hwnd
}

SavePoint(*) {
    global points, pointList, pointPauseInput, newPointPause, statusLabel
    MouseGetPos &x, &y
    points.Push({x: x, y: y, pause: newPointPause})
    RefreshPointsList(points.Length)
    pointPauseInput.Value := newPointPause
    statusLabel.Text := "Added point " points.Length " at X " x ", Y " y "."
    EndPointCapture()
    RestoreApp()
}

CancelPointCapture(*) {
    global statusLabel
    statusLabel.Text := "Point selection cancelled."
    EndPointCapture()
    RestoreApp()
}

EndPointCapture() {
    Hotkey "LButton", SavePoint, "Off"
    Hotkey "Esc", CancelPointCapture, "Off"
}

RestoreApp() {
    global app
    WinRestore "ahk_id " app.Hwnd
    WinActivate "ahk_id " app.Hwnd
    ShowCountdownOverlay()
}

RefreshPointsList(selectRow := 0) {
    global points, pointList
    pointList.Delete()
    for index, point in points {
        row := pointList.Add("", index, point.x, point.y, point.pause)
        if (index = selectRow)
            pointList.Modify(row, "Select Focus Vis")
    }
}

PointSelectionChanged(ctrl, row, selected) {
    global points, pointPauseInput
    if (selected && row >= 1 && row <= points.Length)
        pointPauseInput.Value := points[row].pause
}

SelectedPointRow() {
    global pointList
    return pointList.GetNext()
}

ApplyPauseToSelected(*) {
    global points, pointPauseInput, statusLabel
    row := SelectedPointRow()
    if (!row || row > points.Length) {
        statusLabel.Text := "Select a point in the list first."
        return
    }
    try {
        pauseSeconds := Float(Trim(pointPauseInput.Value))
    } catch {
        statusLabel.Text := "Enter a valid pause in seconds."
        return
    }
    if (pauseSeconds < 0 || pauseSeconds > 600000) {
        statusLabel.Text := "Point pause must be from 0 to 600,000 seconds."
        return
    }
    points[row].pause := pauseSeconds
    RefreshPointsList(row)
    statusLabel.Text := "Pause set to " pauseSeconds " second(s) for point " row "."
}

RemoveSelectedPoint(*) {
    global points, statusLabel
    row := SelectedPointRow()
    if (!row || row > points.Length) {
        statusLabel.Text := "Select a point to remove."
        return
    }
    points.RemoveAt(row)
    RefreshPointsList()
    statusLabel.Text := "Removed point " row "."
}

MoveToSelectedPoint(*) {
    global points, statusLabel
    row := SelectedPointRow()
    if (!row || row > points.Length) {
        statusLabel.Text := "Select a point in the list first."
        return
    }
    MouseMove points[row].x, points[row].y, 0
    statusLabel.Text := "Cursor moved to point " row " and left there."
}

StartClicks(*) {
    global points, running, intervalInput, pointPauseInput, leaveCursorCheckbox
    global stopKeyInput, activeStopHotkey, startButton, stopButton, statusLabel
    global pointList, addPointButton, removePointButton, movePointButton, applyPauseButton
    global presetNameInput, savePresetButton, loadPresetButton
    global clickPeriod, nextClickAt, pointIndex, leaveCursorAtPoint, countdownLabel
    if (points.Length = 0) {
        statusLabel.Text := "Add at least one click point first."
        return
    }
    try {
        seconds := Float(Trim(intervalInput.Value))
    } catch {
        statusLabel.Text := "Enter a valid interval in seconds."
        return
    }
    if (seconds < 0.01 || seconds > 600000) {
        statusLabel.Text := "Enter an interval from 0.01 to 600,000 seconds."
        return
    }
    requestedHotkey := Trim(stopKeyInput.Value)
    if (requestedHotkey = "") {
        statusLabel.Text := "Choose a key for stopping the timer."
        return
    }
    try {
        Hotkey requestedHotkey, StopClicks, "On"
    } catch {
        statusLabel.Text := "That stop key could not be assigned. Choose another key."
        return
    }
    activeStopHotkey := requestedHotkey
    stopKeyInput.Enabled := false
    intervalInput.Enabled := false
    pointPauseInput.Enabled := false
    pointList.Enabled := false
    addPointButton.Enabled := false
    removePointButton.Enabled := false
    movePointButton.Enabled := false
    applyPauseButton.Enabled := false
    presetNameInput.Enabled := false
    savePresetButton.Enabled := false
    loadPresetButton.Enabled := false
    leaveCursorCheckbox.Enabled := false
    clickPeriod := Max(1, Round(seconds * 1000))
    pointIndex := 1
    leaveCursorAtPoint := leaveCursorCheckbox.Value
    running := true
    startButton.Enabled := false
    stopButton.Enabled := true
    statusLabel.Text := "Running through " points.Length " point(s) in order."
    ScheduleNextPoint()
    SetTimer UpdateCountdown, 200
    UpdateCountdown()
}

ScheduleNextPoint() {
    global points, pointIndex, running, clickPeriod, nextClickAt, currentPointPauseMs
    if (!running || points.Length = 0)
        return
    currentPointPauseMs := Round(points[pointIndex].pause * 1000)
    nextClickAt := A_TickCount + Max(clickPeriod, currentPointPauseMs)
    timeUntilMove := Max(1, clickPeriod - currentPointPauseMs)
    SetTimer BeginPointCycle, -timeUntilMove
}

BeginPointCycle() {
    global points, pointIndex, running, returnX, returnY, currentPointPauseMs
    global statusLabel, waitingAfterMove
    if (!running || points.Length = 0)
        return
    point := points[pointIndex]
    MouseGetPos &returnX, &returnY
    MouseMove point.x, point.y, 0
    waitingAfterMove := (currentPointPauseMs > 0)
    if (currentPointPauseMs > 0) {
        statusLabel.Text := "At point " pointIndex ". Waiting " point.pause " second(s) before click."
        UpdateCountdown()
        SetTimer CompletePointCycle, -currentPointPauseMs
    } else {
        CompletePointCycle()
    }
}

CompletePointCycle() {
    global points, pointIndex, running, returnX, returnY, leaveCursorAtPoint
    global waitingAfterMove, statusLabel
    if (!running || points.Length = 0)
        return
    waitingAfterMove := false
    point := points[pointIndex]
    Click
    if (leaveCursorAtPoint) {
        statusLabel.Text := "Clicked point " pointIndex " (X " point.x ", Y " point.y "). Cursor left there."
    } else {
        MouseMove returnX, returnY, 0
        statusLabel.Text := "Clicked point " pointIndex ". Cursor returned to its previous position."
    }
    pointIndex := Mod(pointIndex, points.Length) + 1
    ScheduleNextPoint()
}

UpdateCountdown(*) {
    global running, nextClickAt, countdownLabel
    if (!running) {
        countdownLabel.Text := "--:--"
        return
    }
    remainingSeconds := Max(0, Ceil((nextClickAt - A_TickCount) / 1000))
    countdownLabel.Text := Format("{:02}:{:02}", Floor(remainingSeconds / 60), Mod(remainingSeconds, 60))
}

StopClicks(*) {
    global running, activeStopHotkey, stopKeyInput, intervalInput, pointPauseInput
    global pointList, addPointButton, removePointButton, movePointButton, applyPauseButton
    global presetNameInput, savePresetButton, loadPresetButton, leaveCursorCheckbox
    global startButton, stopButton, statusLabel, countdownLabel
    global waitingAfterMove, returnX, returnY, leaveCursorAtPoint
    SetTimer BeginPointCycle, 0
    SetTimer CompletePointCycle, 0
    SetTimer UpdateCountdown, 0
    if (waitingAfterMove && !leaveCursorAtPoint)
        MouseMove returnX, returnY, 0
    waitingAfterMove := false
    if (activeStopHotkey != "") {
        Hotkey activeStopHotkey, StopClicks, "Off"
        activeStopHotkey := ""
    }
    running := false
    stopKeyInput.Enabled := true
    intervalInput.Enabled := true
    pointPauseInput.Enabled := true
    pointList.Enabled := true
    addPointButton.Enabled := true
    removePointButton.Enabled := true
    movePointButton.Enabled := true
    applyPauseButton.Enabled := true
    presetNameInput.Enabled := true
    savePresetButton.Enabled := true
    loadPresetButton.Enabled := true
    leaveCursorCheckbox.Enabled := true
    startButton.Enabled := true
    stopButton.Enabled := false
    countdownLabel.Text := "STOPPED"
    statusLabel.Text := "Stopped."
}

SavePreset(*) {
    global points, presetDir, presetNameInput, intervalInput, leaveCursorCheckbox, stopKeyInput, statusLabel
    name := Trim(presetNameInput.Value)
    safeName := RegExReplace(name, "[^\w -]", "_")
    if (safeName = "") {
        statusLabel.Text := "Enter a name for this preset."
        return
    }
    if (points.Length = 0) {
        statusLabel.Text := "Add at least one point before saving a preset."
        return
    }
    DirCreate presetDir
    file := presetDir "\" safeName ".ini"
    IniWrite intervalInput.Value, file, "Settings", "IntervalSeconds"
    IniWrite leaveCursorCheckbox.Value, file, "Settings", "LeaveCursorAtPoint"
    IniWrite stopKeyInput.Value, file, "Settings", "StopHotkey"
    IniWrite points.Length, file, "Settings", "PointCount"
    for index, point in points {
        section := "Point" index
        IniWrite point.x, file, section, "X"
        IniWrite point.y, file, section, "Y"
        IniWrite point.pause, file, section, "PauseSeconds"
    }
    statusLabel.Text := "Saved preset " safeName "."
}

LoadPreset(*) {
    global points, presetDir, presetNameInput, intervalInput, leaveCursorCheckbox
    global stopKeyInput, statusLabel, pointPauseInput
    DirCreate presetDir
    file := FileSelect("1", presetDir, "Load Click Setter preset", "Preset files (*.ini)")
    if (file = "")
        return
    try {
        count := Integer(IniRead(file, "Settings", "PointCount", "0"))
        if (count < 1 || count > 100) {
            statusLabel.Text := "That preset has no points or contains too many points."
            return
        }
        loadedPoints := []
        Loop count {
            section := "Point" A_Index
            x := Integer(IniRead(file, section, "X"))
            y := Integer(IniRead(file, section, "Y"))
            pauseSeconds := Float(IniRead(file, section, "PauseSeconds", "0.5"))
            if (pauseSeconds < 0 || pauseSeconds > 600000)
                throw Error("Invalid point pause")
            loadedPoints.Push({x: x, y: y, pause: pauseSeconds})
        }
        points := loadedPoints
        intervalInput.Value := IniRead(file, "Settings", "IntervalSeconds", "10")
        leaveCursorCheckbox.Value := IniRead(file, "Settings", "LeaveCursorAtPoint", "0")
        stopKeyInput.Value := IniRead(file, "Settings", "StopHotkey", "F8")
        pointPauseInput.Value := points[1].pause
        SplitPath file, , , , &presetName
        presetNameInput.Value := presetName
        RefreshPointsList(1)
        statusLabel.Text := "Loaded preset " presetName "."
    } catch {
        statusLabel.Text := "Could not read that preset. Choose a Click Setter preset file."
    }
}
