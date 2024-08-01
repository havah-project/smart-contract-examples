# vesting

This subproject provides a sample implementation of vesting.

 
## Vesting Type    
All vesting types have a start time and an end time.   
This contract automatically calculates the time and quantity that can be claimed using the vesting type and the start-end time.

### Onetime
Claim all amounts after start time.

### Liner
Claims as a percentage of the current time between start and end times.

### Periodic
Allocate the claim amount equally between the start and end times by a given time interval.
For example, if the conditions are as follows
```
* Start time: 01:15:00
* End time: 01:48:00
* Interval : 10 minute    
``` 
Claims can be made equally at the following times
```
* 01:15:00
* 01:25:00
* 01:35:00
* 01:45:00
```

### Daily
Claims can be made once a day between start and end times. Time can be specified in hours from 0 to 23
For example, if the conditions are as follows
```
* Start time : 2024-10-10 15:00:00
* End time : 2024-10-40 15:00:00
* Hour : 18
```
Claims can be made equally at the following times
```
* 2024-10-21 18:00:00
* 2024-10-22 18:00:00
* 2024-10-23 18:00:00
```
### Weekly
Claims can be made once a week at a specified day between start and end times.   
Day of the week value: Sunday: 0, Monday: 1 to Saturday: 6   
```
* Start time : 2025-01-06 15:00:00
* End time : 2025-01-27 15:00:00
* Day of week : 2 (Tuesday)
* Hour 18
```
Claims can be made equally at the following times
```
* 2025-01-07 18:00:00
* 2025-01-14 18:00:00
* 2025-01-21 18:00:00
```

### Monthly
Claims can be made once a month at a specified date between start and end times.   
Dates can be entered from 1st to 31st. If it is a month without that date (for example, 29th, 30th, 31st), it will be claimed on the 1st of the following month.

For example, if the conditions are as follows
```
* Start time : 2025-01-10 00:00:00
* End time : 2025-05-10 23:59:59
* Day : 30
* Hour : 9
```
Claims can be made equally at the following times
```
* 2025-01-30 09:00:00
* 2025-03-01 09:00:00
* 2025-03-30 09:00:00
* 2025-04-30 09:00:00
```

### Yearly
Claims can be made on a specified date once a year between start and end times.   
For months without dates, follow the rules of Monthly type above.   
For example, if the conditions are as follows
```
* Start time : 2024-01-10 00:00:00
* End time : 2026-08-10 23:59:59
* Month : 5
* Day : 22
* Hour : 9
```
Claims can be made equally at the following times
```
* 2024-05-22 09:00:00
* 2025-05-22 09:00:00
* 2026-05-22 09:00:00
```

