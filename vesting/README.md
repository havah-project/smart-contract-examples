# Vesting

This subproject provides a sample implementation of vesting. The vesting contract supports various vesting types to be distributed to recipients. 
The recipient must claim their rewards, and it automatically calculates the vested amount according to the vesting type, the current time, and the total amount of the vesting.

## How to Use
1. Create a vesting by <code>registerXXXVesting()</code> according to the vesting type.
1. Add or remove reward recipients by <code>add/removeVestingAccounts()</code>
1. Recipients can claim the amount determined based on the total reward and the vesting schedule.

## Vesting Type    

### Onetime
The total amount is claimable after the specified time. Usually called cliff vesting.

### Liner
The claimable amount is determined based on the proportion of the current time relative to the start and end times.

### Periodic
The amount is determined gradually at regular intervals. Note that the claimable amount is incremented at the start of each interval.
For example, if the vesting schedule is as follows.
```
* Start time: 01:15:00
* End time: 01:48:00
* Interval : 10 minute    
``` 
The claimable amount are newly allowed at the following times. 
```
* 01:15:00
* 01:25:00
* 01:35:00
* 01:45:00
```

### Daily
The claimable amount is incremented daily at a specified time between the start and end times. The time is hour-basis ranging from 0 to 23 on UTC.
For example, if the vesting schedule is as follows.
```
* Start time : 2024-10-10 15:00:00
* End time : 2024-10-40 15:00:00
* Hour : 18
```
The claimable amount are newly allowed at the following times. 
```
* 2024-10-21 18:00:00
* 2024-10-22 18:00:00
* 2024-10-23 18:00:00
```

### Weekly
The claimable amount is incremented weekly at a specified day and time between the start and end times. The day is ranging from 0 to 6.
Day of the week value: Sunday(0), Monday(1), ..., Saturday(6)   
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
The claimable amount is incremented monthly at a specified day and time between the start and end times. The dates ranges from the 1st to the 31st. If the specified date does not exist in the month, the claimable amount is incremented on the 1st day of the following month.

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
The claimable amount is incremented yearly at a specified day and time between the start and end times. If the specified date does not exist in the year, the claimable amount is incremented on the 1st day of the following month.
For example, if the conditions are as follows
```
* Start time : 2024-01-10 00:00:00
* End time : 2026-08-10 23:59:59
* Month : 2
* Day : 29
* Hour : 9
```
Claims can be made equally at the following times
```
* 2024-02-29 09:00:00
* 2025-03-01 09:00:00
* 2026-03-01 09:00:00
```


