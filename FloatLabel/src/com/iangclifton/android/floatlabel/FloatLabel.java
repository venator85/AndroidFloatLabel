package com.iangclifton.android.floatlabel;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * A ViewGroup that consists of an EditText and a TextView as the label.<br/>
 * <br/>
 * When the EditText is empty, its hint is displayed. When it is nonempty, the
 * TextView label is displayed.<br/>
 * <br/>
 * You can set the label programmatically with either
 * {@link #setLabel(CharSequence)} or {@link #setLabel(int)}.
 * 
 * @author Ian G. Clifton
 * @see <a
 *      href="http://dribbble.com/shots/1254439--GIF-Float-Label-Form-Interaction">Float
 *      label inspiration on Dribbble</a>
 */
public class FloatLabel extends FrameLayout {

	private static final String SAVE_STATE_KEY_EDIT_TEXT = "saveStateEditText";
	private static final String SAVE_STATE_KEY_LABEL = "saveStateLabel";
	private static final String SAVE_STATE_PARENT = "saveStateParent";
	private static final String SAVE_STATE_TAG = "saveStateTag";

	private Bundle mSavedState;

	private EditText mEditText;
	private TextView mLabel;

	private boolean mLabelShowing;

	public FloatLabel(Context context) {
		super(context);
		init(context, null, 0);
	}

	public FloatLabel(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0);
	}

	public FloatLabel(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	private void init(Context context, AttributeSet attrs, int defStyle) {

		mLabel = new TextView(context);
		addView(mLabel, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	}

	private void setEditText(EditText editText) {

		mEditText = editText;

		mLabel.setText(mEditText.getHint());

		mEditText.addTextChangedListener(new EditTextWatcher());
		if (mEditText.getText().length() == 0) {
			mLabel.setAlpha(0);
			mLabelShowing = false;
		} else {
			mLabel.setVisibility(View.VISIBLE);
			mLabelShowing = true;
		}
	}

	public EditText getEditText() {
		return mEditText;
	}

	public TextView getLabel() {
		return mLabel;
	}

	@Override
	public final void addView(View child, int index, ViewGroup.LayoutParams params) {
		if (child instanceof EditText) {
			if (mEditText != null) {
				throw new IllegalArgumentException("We already have an EditText, can only have one");
			}
			setEditText((EditText) child);
		}
		super.addView(child, index, params);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int childLeft = getPaddingLeft();
		final int childRight = right - left - getPaddingRight();

		int childTop = getPaddingTop();
		final int childBottom = bottom - top - getPaddingBottom();

		layoutChild(mLabel, childLeft, childTop, childRight, childBottom);
		layoutChild(mEditText, childLeft, childTop + mLabel.getMeasuredHeight(), childRight, childBottom);
	}

	@SuppressLint("RtlHardcoded")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	private void layoutChild(View child, int parentLeft, int parentTop, int parentRight, int parentBottom) {
		if (child.getVisibility() != GONE) {
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();

			final int width = child.getMeasuredWidth();
			final int height = child.getMeasuredHeight();

			int childLeft;
			final int childTop = parentTop + lp.topMargin;

			int gravity = lp.gravity;
			if (gravity == -1) {
				gravity = Gravity.TOP | Gravity.START;
			}

			final int layoutDirection;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
				layoutDirection = LAYOUT_DIRECTION_LTR;
			} else {
				layoutDirection = getLayoutDirection();
			}

			final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);

			switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
				case Gravity.CENTER_HORIZONTAL:
					childLeft = parentLeft + (parentRight - parentLeft - width) / 2 + lp.leftMargin - lp.rightMargin;
					break;
				case Gravity.RIGHT:
					childLeft = parentRight - width - lp.rightMargin;
					break;
				case Gravity.LEFT:
				default:
					childLeft = parentLeft + lp.leftMargin;
			}

			child.layout(childLeft, childTop, childLeft + width, childTop + height);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Restore any state that's been pending before measuring
		if (mSavedState != null) {
			Parcelable childState = mSavedState.getParcelable(SAVE_STATE_KEY_EDIT_TEXT);
			mEditText.onRestoreInstanceState(childState);
			childState = mSavedState.getParcelable(SAVE_STATE_KEY_LABEL);
			mLabel.onRestoreInstanceState(childState);
			mSavedState = null;
		}
		measureChild(mEditText, widthMeasureSpec, heightMeasureSpec);
		measureChild(mLabel, widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			final Bundle savedState = (Bundle) state;
			if (savedState.getBoolean(SAVE_STATE_TAG, false)) {
				// Save our state for later since children will have theirs restored after this
				// and having more than one FloatLabel in an Activity or Fragment means you have
				// multiple views of the same ID
				mSavedState = savedState;
				super.onRestoreInstanceState(savedState.getParcelable(SAVE_STATE_PARENT));
				return;
			}
		}
		super.onRestoreInstanceState(state);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		final Bundle saveState = new Bundle();
		saveState.putParcelable(SAVE_STATE_KEY_EDIT_TEXT, mEditText.onSaveInstanceState());
		saveState.putParcelable(SAVE_STATE_KEY_LABEL, mLabel.onSaveInstanceState());
		saveState.putBoolean(SAVE_STATE_TAG, true);
		saveState.putParcelable(SAVE_STATE_PARENT, superState);

		return saveState;
	}

	private int measureHeight(int heightMeasureSpec) {
		int specMode = MeasureSpec.getMode(heightMeasureSpec);
		int specSize = MeasureSpec.getSize(heightMeasureSpec);

		int result = 0;
		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			result = mEditText.getMeasuredHeight() + mLabel.getMeasuredHeight();
			result += getPaddingTop() + getPaddingBottom();
			result = Math.max(result, getSuggestedMinimumHeight());

			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}

		return result;
	}

	private int measureWidth(int widthMeasureSpec) {
		int specMode = MeasureSpec.getMode(widthMeasureSpec);
		int specSize = MeasureSpec.getSize(widthMeasureSpec);

		int result = 0;
		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			result = Math.max(mEditText.getMeasuredWidth(), mLabel.getMeasuredWidth());
			result = Math.max(result, getSuggestedMinimumWidth());
			result += getPaddingLeft() + getPaddingRight();
			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}

		return result;
	}

	private class EditTextWatcher implements TextWatcher {

		private boolean hidePending;
		private Integer hintTextColor;

		private int HIDE_DELAYER_CODE = 14672;

		/**
		 * Introduce a delay of 150 ms before hiding the label.
		 * This is to compensate repeated unwanted calls to afterTextChanged() when pressing backspace.
		 * Reproducible by typing "a " (note the space at the end) and deleting one char.
		 * Android will call afterTextChanged() three (!) times, passing "a", then "" (hides the label),
		 * then "a" (shows the label).
		 */
		@SuppressLint("HandlerLeak") private Handler hideDelayer = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (msg.what == HIDE_DELAYER_CODE) {
					if (mLabelShowing) {
						hideLabel(mEditText, mLabel);
						mLabelShowing = false;
					}
					hidePending = false;
				}
			}
		};

		@Override
		public void afterTextChanged(Editable s) {

			if (hintTextColor == null) {
				hintTextColor = mEditText.getCurrentHintTextColor();
			}
			if (s.length() == 0) {
				// Text is empty; TextView label should be invisible
				hidePending = true;
				mEditText.setHintTextColor(Color.TRANSPARENT);
				hideDelayer.removeMessages(HIDE_DELAYER_CODE);
				hideDelayer.sendEmptyMessageDelayed(HIDE_DELAYER_CODE, 150);
			} else if (!mLabelShowing) {
				// Text is nonempty; TextView label should be visible
				mLabelShowing = true;
				displayLabel(mEditText, mLabel);
			} else if (hidePending) {
				hideDelayer.removeMessages(HIDE_DELAYER_CODE);
				hidePending = false;
			}
		}

		private void displayLabel(EditText editText, TextView label) {
			final float offset = label.getHeight() / 2;
			final float currentY = label.getY();
			if (currentY != offset) {
				label.setY(offset);
			}
			label.animate().setDuration(300).alpha(1).y(0);
		}

		private void hideLabel(EditText editText, TextView label) {
			final float offset = label.getHeight() / 2;
			final float currentY = label.getY();
			if (currentY != 0) {
				label.setY(0);
			}
			label.animate().setDuration(300).alpha(0).y(offset);

			ValueAnimator fadeInHint = ValueAnimator.ofObject(new ColorFadeInEvaluator(hintTextColor), 0);
			fadeInHint.setDuration(300);
			fadeInHint.start();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// Ignored
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// Ignored
		}
	}

	private final class ColorFadeInEvaluator implements TypeEvaluator<Integer> {
		private final int mHintTextColor;

		public ColorFadeInEvaluator(int hintTextColor) {
			mHintTextColor = hintTextColor;
		}

		@Override
		public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
			int endAlpha = mHintTextColor >> 24 & 0xff;
			int endColor = mHintTextColor & 0xffffff;
			int outColor = (int) (fraction * endAlpha) << 24 | endColor;
			mEditText.setHintTextColor(outColor);
			return outColor;
		}
	}

}
