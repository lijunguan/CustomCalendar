package com.example.calendar_agenda.week;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.calendar_agenda.util.ProxyOnDaySelectedListener;

import org.threeten.bp.Clock;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by lijunguan on 2017/1/17. email: lijunguan199210@gmail.com blog:
 * https://lijunguan.github.io
 */

public class WeekAdapter extends PagerAdapter {

    private static final int DAYS_IN_WEEK = 7;
    //日历起始日期
    private LocalDate mMinDate = LocalDate.MIN;
    //日历截止日期
    private LocalDate mMaxDate = LocalDate.MAX;

    // 周视图,起始的日期(可能比最小值小)
    private LocalDate mStartDate;

    private final SparseArray<WeekView> mWeekViews = new SparseArray<>();

    private int mCount;

    private final LayoutInflater mInflater;
    private final int mLayoutResId;
    private final int mWeekViewId;


    private LocalDate mSelectedDay = null;

    private SparseArray<List<Integer>> mMonthEvents;

    /**
     * 设置的起始日期的偏移量, 即最小日期所在的周,距离周的第一天的偏移量.
     */
    private int mOffset;


    private ProxyOnDaySelectedListener mOnDaySelectedListener;

    public WeekAdapter(@NonNull Context context, @LayoutRes int layoutResId,
                       @IdRes int calendarViewId) {
        mInflater = LayoutInflater.from(context);
        mLayoutResId = layoutResId;
        mWeekViewId = calendarViewId;

    }

    /**
     * 日历设置的 周首日 (eg US 周日开头, 法国 周一开头)
     */
    private int mWeekStart;

    public void setRange(@NonNull LocalDate min, @NonNull LocalDate max) {
        mMinDate = min;
        mMaxDate = max;
        int days = (int) min.until(max, ChronoUnit.DAYS);

        final int dayOfWeek = min.getDayOfWeek().getValue(); //最小日期是周几
        //计算偏移量
        mOffset = dayOfWeek - mWeekStart;
        if (dayOfWeek < mWeekStart) {
            mOffset += DAYS_IN_WEEK;
        }
        mStartDate = mMinDate.minusDays(mOffset);
        mCount = (days + mOffset) / DAYS_IN_WEEK;

        // Positions are now invalid, clear everything and start over.
        notifyDataSetChanged();

    }


    /**
     * Sets the first day of the week.
     *
     * @param weekStart which day the week should start on, valid values are {@link Calendar#SUNDAY}
     *                  through {@link Calendar#SATURDAY}
     */
    public void setWeekStart(int weekStart) {
        mWeekStart = weekStart;
        // Update displayed views.
        final int count = mWeekViews.size();
        for (int i = 0; i < count; i++) {
            final WeekView weekView = mWeekViews.valueAt(i);
            weekView.setFirstDayOfWeek(weekStart);
        }
    }


    public void setOnDaySelectedListener(ProxyOnDaySelectedListener listener) {
        mOnDaySelectedListener = listener;
    }


    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final View itemView = mInflater.inflate(mLayoutResId, container, false);
        final WeekView weekView = (WeekView) itemView.findViewById(mWeekViewId);
        weekView.setOnDayClickListener(mOnDayClickListener);
        LocalDate startDayOfWeek = mStartDate.plusWeeks(position);

        weekView.setWeekParams(startDayOfWeek, mSelectedDay, mMinDate, mMaxDate, mWeekStart, mMonthEvents);
        mWeekViews.put(position, weekView);
        container.addView(weekView);
        return weekView;
    }

    public void setMonthEvents(int month, @NonNull List<Integer> events) {
        mMonthEvents.put(month, events);
        notifyDataSetChanged();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        mWeekViews.remove(position);
    }

    /**
     * Sets the selected day.
     *
     * @param day the selected day
     */
    public void setSelectedDay(@Nullable LocalDate day) {

        final int oldPosition = getPositionForDay(mSelectedDay);
        final int newPosition = getPositionForDay(day);

        // Clear the old position if necessary.
        if (oldPosition != newPosition && oldPosition >= 0) {
            final WeekView oldWeekView = mWeekViews.get(oldPosition, null);
            if (oldWeekView != null) {
                oldWeekView.setSelectedDay(null);
            }
        }

        // Set the new position.
        if (newPosition >= 0) {
            final WeekView newWeekView = mWeekViews.get(oldPosition, null);
            if (newWeekView != null) {
                newWeekView.setSelectedDay(day);
            }
        }

        mSelectedDay = day;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        WeekView weekView = mWeekViews.get(position);
        if (weekView != null) {
            return weekView.getMonthYearLabel();
        }
        return "";
    }

    /**
     * 根据日期得到 该日期对应的周的position
     *
     * @param date 日期对象
     * @return 传入日期对应的周的 position, 加入了校验 返回的position 在 0 ~ mCount -1
     */
    public int getPositionFromDay(LocalDate date) {
        final int totalWeeks = getCount();
        final int offsetDays = (int) mMinDate.until(date, ChronoUnit.DAYS);
        final int position = (offsetDays + mOffset) / DAYS_IN_WEEK;
        return constrain(position, 0, totalWeeks - 1);
    }

    private int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public int getPositionForDay(@Nullable LocalDate day) {
        if (day == null) {
            return -1;
        }

        int offsetDays = (int) mMinDate.until(day, ChronoUnit.DAYS);
        //根据日期
        return (offsetDays + mOffset) / DAYS_IN_WEEK;
    }


    private final WeekView.OnDayClickListener mOnDayClickListener = new WeekView.OnDayClickListener() {
        @Override
        public void onDayClick(WeekView view, LocalDate day) {
            if (day != null) {
                setSelectedDay(day);

                if (mOnDaySelectedListener != null) {
                    mOnDaySelectedListener.onDaySelected(WeekAdapter.this, day);
                }
            }
        }
    };

    public LocalDate getStartDayOfWeek(int position) {
        WeekView weekView = mWeekViews.get(position);
        if (weekView != null) {
            return weekView.getStartDayOfWeek();
        }
        return null;
    }


}
