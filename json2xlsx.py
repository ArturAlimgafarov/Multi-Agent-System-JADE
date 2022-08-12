import pandas as pd
import json

with open('output.json', 'r', encoding='utf-8') as f:
    fileData = json.load(f)

maxDuration = max((sum((dur for _, dur in ords.items())) for _, ords in fileData.items()))
data = []
for _, orders in fileData.items():
    schedule = []
    sumDur = maxDuration
    for key, duration in orders.items():
        schedule += [key for _ in range(duration)]
        sumDur -= duration
    schedule += ['free' for _ in range(sumDur)]
    data.append(schedule)

pd.DataFrame(
    data,
    index=(name for name, _ in fileData.items()),
    columns=tuple(range(1, maxDuration + 1))
).to_excel('output.xlsx')
