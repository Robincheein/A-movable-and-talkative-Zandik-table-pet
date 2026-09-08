$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
$utf8 = New-Object System.Text.UTF8Encoding($false)
$behaviorPath = Join-Path (Get-Location) 'conf/Behavior.xml'
$behavior = [IO.File]::ReadAllText($behaviorPath)
# 只注释行为入口，保留 Actions.xml 中原动作定义，之后可以恢复。
$behavior = [regex]::Replace($behavior, '(?m)^(\s*)<行動 名前="([^"]+)"[^>]*?/>', {
    param($match)
    if ($match.Groups[2].Value -match '壁|登る|よじのぼる|掴まって|上辺を伝う|下辺を伝う|飛びつく') {
        return $match.Groups[1].Value + '<!-- ANIMAL_V1_DISABLED: ' + $match.Value.Trim() + ' -->'
    }
    return $match.Value
})
[IO.File]::WriteAllText($behaviorPath, $behavior, $utf8)
$actionPath = Join-Path (Get-Location) 'conf/Actions.xml'
$actions = [IO.File]::ReadAllText($actionPath)
# 所有地面姿势共用脚底基准，避免换姿势时上下跳动。
$actions = $actions.Replace('基準座標="64,112"','基準座標="64,128"').Replace('基準座標="64,104"','基準座標="64,128"')
foreach ($name in @('歩く','走る','猛ダッシュ')) {
    $pattern = '(?s)(<動作 名前="' + $name + '"[^>]*>)(.*?)(</動作>)'
    $actions = [regex]::Replace($actions, $pattern, {
        param($match)
        $counter = @{ value = 0 }
        $body = [regex]::Replace($match.Groups[2].Value, '画像="/[^"]+"', {
            param($pose)
            $counter.value++
            return '画像="/fox_walk_0' + $counter.value + '.png"'
        })
        return $match.Groups[1].Value + $body + $match.Groups[3].Value
    })
}
# 拖起后始终使用悬空姿势，不再突然闪回坐姿。
$actions = [regex]::Replace($actions, '(?s)(<動作 名前="(?:つままれる|抵抗する)"[^>]*>)(.*?)(</動作>)', {
    param($match)
    return $match.Groups[1].Value + [regex]::Replace($match.Groups[2].Value, '画像="/[^"]+"', '画像="/shime5.png"') + $match.Groups[3].Value
})
[IO.File]::WriteAllText($actionPath, $actions, $utf8)
Copy-Item conf/Actions.xml,conf/Behavior.xml -Destination dist/conf -Force
Write-Host 'ANIMAL_CONFIG_OK'
