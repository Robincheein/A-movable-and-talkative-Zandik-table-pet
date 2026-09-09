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
foreach ($name in @('歩く','走る','猛ダッシュ','IEを持って歩く','IEを持って走る')) {
    $pattern = '(?s)(<動作 名前="' + $name + '"[^>]*>)(.*?)(</動作>)'
    $actions = [regex]::Replace($actions, $pattern, {
        param($match)
        $body = "`n`t`t`t<アニメーション>`n"
        foreach ($frame in 1..8) {
            $body += "`t`t`t`t<ポーズ 画像=`"/fox_walk_0$frame.png`" 基準座標=`"64,128`" 移動速度=`"-1,0`" 長さ=`"4`" />`n"
        }
        $body += "`t`t`t</アニメーション>`n`t`t"
        return $match.Groups[1].Value + $body + $match.Groups[3].Value
    })
}
# 拖起后始终使用悬空姿势，不再突然闪回坐姿。
$actions = [regex]::Replace($actions, '(?s)(<動作 名前="(?:つままれる|抵抗する)"[^>]*>)(.*?)(</動作>)', {
    param($match)
    return $match.Groups[1].Value + [regex]::Replace($match.Groups[2].Value, '画像="/[^"]+"', '画像="/shime5.png"') + $match.Groups[3].Value
})
[IO.File]::WriteAllText($actionPath, $actions, $utf8)
# 落地使用站姿缓冲；睡觉图片仍保留给真正的睡眠动作。
$actions = [regex]::Replace($actions, '(?s)<動作 名前="跳ねる"[^>]*>.*?</動作>', @'
<動作 名前="跳ねる" 種類="組み込み" クラス="com.group_finity.mascot.action.Landing" 枠="地面">
            <アニメーション>
                <ポーズ 画像="/fox_stand.png" 基準座標="64,128" 移動速度="0,0" 長さ="8" />
            </アニメーション>
        </動作>
'@)
# Dragged 本身持续至松开，保留抵抗する定义但不再随机切入。
$actions = [regex]::Replace($actions, '(?s)(<動作 名前="ドラッグされる"[^>]*>).*?(</動作>)', '$1<動作参照 名前="つままれる" />$2')
[IO.File]::WriteAllText($actionPath, $actions, $utf8)
# 保留行为定义，但停用随机快跑、扭头快速换图、抖脚及趴着滑行。
# 多个地面移动动作仍统一使用八帧慢走，避免外部入口恢复后突然加速。
$behavior = [regex]::Replace($behavior, '(<(?:行動|行動参照) 名前="[^"]*(?:走る|走って|首が回った|足をぶらぶら|ずりずり)[^"]*" 頻度=")[0-9]+("[^>]*>)', '${1}0${2}')
[IO.File]::WriteAllText($behaviorPath, $behavior, $utf8)
Copy-Item conf/Actions.xml,conf/Behavior.xml -Destination dist/conf -Force
Write-Host 'ANIMAL_CONFIG_OK'
