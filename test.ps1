# Windows 版 test.sh：把 4 种 swap 方案 × 3 组应用各跑一轮。
#
# 用法（在项目根目录）：
#   .\test.ps1                # 全部 12 轮
#   .\test.ps1 -SwapType 1    # 只跑 zRAM
#   .\test.ps1 -SwapType 2 -AppSet 0
#
# 一轮大约 5~10 分钟（取决于应用数和轮数），全部跑完要一小时以上。

param(
    [ValidateRange(0, 3)][int[]] $SwapType = 0, 1, 2, 3,
    [ValidateRange(0, 2)][int[]] $AppSet = 0, 1, 2
)

$swapNames = @{ 0 = "no_swap"; 1 = "only_zram"; 2 = "flash_swap"; 3 = "zswap" }

foreach ($type in $SwapType) {
    foreach ($set in $AppSet) {
        $name = $swapNames[$type]
        Write-Host "=== swap=$name (t=$type), app-set=$set ===" -ForegroundColor Cyan
        .\gradlew.bat run --args="-t $type -p $name -a $set"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "gradle run 失败（exit=$LASTEXITCODE），已中止后续批次。" -ForegroundColor Red
            exit $LASTEXITCODE
        }
    }
}

Write-Host "全部批次执行完毕，结果在 logs\<时间戳>_<方案>_<应用组>\ 下。" -ForegroundColor Green
