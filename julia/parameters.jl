
# 两个轨迹点之间超过 600 秒（10 分钟），认为开始休息
# 2-5 分钟不够（等红灯、路边临停），10 分钟以上才是真正休息
const MAX_GAP_SECONDS = 600
# 休息时长少于 10 分钟不计为休息（排除短暂停靠）
const MIN_DURATION_MINUTES = 10
# 休息时长超过 480 分钟（8 小时）不计为休息（可能是跨天数据异常）
const MAX_DURATION_MINUTES = 480
