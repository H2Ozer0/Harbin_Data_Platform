export const dashboardChartTheme = {
  backgroundColor: 'transparent',
  textStyle: { color: '#fff' },
  tooltip: {
    backgroundColor: 'rgba(10,14,26,0.9)',
    borderColor: '#0cf',
    textStyle: { color: '#fff' }
  },
  legend: {
    textStyle: { color: 'rgba(255,255,255,0.7)' }
  },
  xAxis: {
    axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
    axisLabel: { color: 'rgba(255,255,255,0.7)' },
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
  },
  yAxis: {
    axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
    axisLabel: { color: 'rgba(255,255,255,0.7)' },
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
  },
  series: [
    {
      itemStyle: { color: '#0cf' },
      lineStyle: { color: '#0cf' },
      areaStyle: { color: 'rgba(0,204,255,0.2)' }
    }
  ]
};

export const dashboardColors = ['#0cf', '#f0c', '#fc0', '#0fc', '#cf0', '#f90', '#9f0'];
