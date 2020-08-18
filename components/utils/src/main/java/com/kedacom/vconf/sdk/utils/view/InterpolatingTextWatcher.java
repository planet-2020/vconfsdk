package com.kedacom.vconf.sdk.utils.view;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.log.KLog;


/**
 * 插值文本监听器。
 * 该监听器针对输入的text根据指定的span插入指定的separator。
 * NOTE：InputFilter不能过滤掉separator中的字符，否则该监听器不能正常工作。
 * */
public final class InterpolatingTextWatcher implements TextWatcher {
    // 原始的文本（剔除插值）
    private StringBuilder rawText = new StringBuilder();
    // 添加插值后的文本
    private StringBuilder interpolatedText = new StringBuilder();
    // 删掉的内容
    private StringBuilder deletedText = new StringBuilder();

    private int cursorPos;

    private final EditText editText;
    private final int[] spans;
    private final String[] separators;
    private final int till;
    private final boolean rejectInputSeparator;

    public static final int TILL_END = Integer.MAX_VALUE;

    /**
     * @param editText 监听的EditText
     * @param spans 插入的跨度
     * @param separators 插入的字符串
     * @param till 计算的结束位置。若大于text长度则取text长度值；取值{@link #TILL_END}表示到text末尾。
     * @param rejectInputSeparator 是否丢弃用户输入的插值字符。如：若插值为"-"，用户输入"1-2"，则为true时实际处理的text为"12"（剔除了'-'），为false则维持原样。
     *
     * 举例：若editText输入内容“1234567”，span为{1}，separator为{"-"}，till=7, 则最终展示在EditText上的内容为“1-2-3-4-5-6-7”。
     *      若editText输入内容“1234567”，span为{1}，separator为{"--"}，till=7, 则最终展示在EditText上的内容为“1--2--3--4--5--6--7”。
     *      若editText输入内容“1234567”，span为{1,2}，separator为{"-"}，till=7, 则最终展示在EditText上的内容为“1-23-45-67”。
     *      若editText输入内容“1234567”，span为{1}，separator为{"-",":"}，till=7, 则最终展示在EditText上的内容为“1-2:3:4:5:6:7”。
     *      若editText输入内容“1234567”，span为{1,2}，separator为{"-",":"}，till=7, 则最终展示在EditText上的内容为“1-23:45:67”。
     *      若editText输入内容“1234567”，span为{1,2}，separator为{"-",":"}，till=4, 则最终展示在EditText上的内容为“1-23:4567”。
     *      若editText输入内容“123456-7”，span为{2}，separator为{"-",":"}，till=4, rejectInputSeparator=true, 则最终展示在EditText上的内容为“12-34567”。
     *      若editText输入内容“123456-7”，span为{2}，separator为{"-",":"}，till=5, rejectInputSeparator=false, 则最终展示在EditText上的内容为“12-34:56-7”。
     *     */
    public InterpolatingTextWatcher(@NonNull EditText editText, @NonNull int[] spans, @NonNull String[] separators, int till, boolean rejectInputSeparator) {
        this.editText = editText;
        this.spans = new int[spans.length];
        this.separators = new String[separators.length];
        System.arraycopy(spans, 0, this.spans, 0, spans.length);
        System.arraycopy(separators, 0, this.separators, 0, separators.length);

        this.till = till>0 ? till : TILL_END;

        this.rejectInputSeparator = rejectInputSeparator;

        StringBuilder sb = new StringBuilder();
        sb.append("spans:[");
        for (int span : spans) {
            sb.append(span).append(",");
        }
        sb.append("], ");
        sb.append("separators:[");
        for (String separator : separators) {
            sb.append(separator).append(", ");
        }
        sb.append("], ");
        sb.append("till: ").append(till);

        KLog.p(sb.toString());
    }

    public InterpolatingTextWatcher(@NonNull EditText editText, @NonNull int[] spans, @NonNull String[] separators, boolean rejectInputSeparator) {
        this(editText, spans, separators, TILL_END, rejectInputSeparator);
    }

    public InterpolatingTextWatcher(@NonNull EditText editText, @NonNull int[] spans, @NonNull String[] separators) {
        this(editText, spans, separators, true);
    }


    @Override
    public void beforeTextChanged(
            CharSequence s, //NOTE: 此处进来的s是经过TextFilter过滤后的
            int start, int count, int after) {
        KLog.p("s=%s, start=%s, count=%s, after=%s", s, start, count, after);
        rawText.delete(0, rawText.length());
        rawText.append(s);
        deletedText.delete(0, deletedText.length());
        deletedText.append(s.subSequence(0, start+count));


        cursorPos = start;
        for (int i=0, span=spans[i];
             span<Math.min(till, rawText.length());
             ++i, span += i<spans.length ? spans[i] : spans[spans.length-1]){
            KLog.p("spans[%s]=%s, rawText=%s, cursorPos=%s", i, span, rawText, cursorPos);
            String separator = i<separators.length ? separators[i] : separators[separators.length-1];
            int separatorLen = separator.length();
            rawText.delete(span, span+separatorLen);
            if (span< deletedText.length()) {
                deletedText.delete(span, span+separatorLen);
            }
            if (span< cursorPos || (span == cursorPos && count > 0)){
                cursorPos -= separatorLen;
            }
        }

        deletedText.delete(0, cursorPos);
        KLog.p("rawText=%s, cursorPos=%s, deletedText=%s,", rawText, cursorPos, deletedText);
        if (deletedText.length() != 0) {
            rawText.delete(cursorPos, cursorPos + deletedText.length());
        }
        KLog.p("rawDeletedPart=%s, cursorPos=%s, rawText=%s,", deletedText, cursorPos, rawText);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        KLog.p("s=%s, start=%s, before=%s, count=%s, rawText=%s", s, start, before, count, rawText);

        CharSequence addedText = s.subSequence(start, start+count);
        if (rejectInputSeparator){
            String text = addedText.toString();
            for (String sep : separators){
                text = text.replace(sep, "");
            }
            KLog.p("addedText=%s, after polish =%s", addedText, text);
            addedText = text;
        }

        rawText.insert(cursorPos, addedText);
        cursorPos += addedText.length();

        KLog.p("rawText=%s, addedText=%s, cursorPos=%s", rawText, addedText, cursorPos);
    }


    @Override
    public void afterTextChanged(Editable s) {
        KLog.p("s=%s,", s);
        interpolatedText.delete(0, interpolatedText.length());
        interpolatedText.append(rawText);

        // 对原始text进行插值处理生成插值后的text
        int stop = Math.min(till, rawText.length());
        KLog.p("till=%s, spans[0]=%s, rawText=%s,", stop, spans[0], rawText);
        for (int i=0, span=spans[i];
             span<Math.min(stop, interpolatedText.length());
             ++i, span += i<spans.length ? spans[i] : spans[spans.length-1]){
            String sep = i<separators.length ? separators[i] : separators[separators.length-1];
            int sepLen = sep.length();
            interpolatedText.insert(span, sep);
            if (span<= cursorPos){
                cursorPos += sepLen;
            }
            span += sepLen;
            stop += sepLen;
            KLog.p("span=%s, stop=%s, sep=%s, cursorPos=%s, interpolatedText=%s,", span, stop, sep, cursorPos, interpolatedText);
        }

        // setText的行为是先触发“beforeTextChanged->onTextChanged->afterTextChanged”然后才继续往下执行，这非我们期望，故暂时删除listener
        editText.removeTextChangedListener(this);
        s.clear();
        s.append(interpolatedText);
        editText.addTextChangedListener(this);
        KLog.p("rawText=%s, interpolatedText=%s, editText.getText()=%s, cursorPos=%s",
                rawText, interpolatedText, editText.getText(), cursorPos);
        editText.setSelection(Math.min(cursorPos, editText.getText().length())); // 更新光标位置。由于我们修改了Text内容原来的光标位置也需相应更新
    }

    /**
     * 获取关联的EditText的text，包含了插值。
     * */
    public String getText(){
        return editText.getText().toString();
    }

    /**
     * 获取关联的EditText的text，不包含插值。
     * */
    public String getRawText(){
        return rawText.toString();
    }


    /**
     * 获取EditText实际允许输入的最大长度（包含插值）。该长度可用于{@link android.text.InputFilter.LengthFilter}
     * @param maxInputTextLen 允许用户输入的最大文本长度
     * @return 实际的添加插值后的最大文本长度。
     * 例如：
     * 一个输入手机号的EditText，规定最多输入11位数字，插值后的文本格式需是"123-4567-8901"，即spans={3,4}, separators={"-"}，
     * 则用户需创建如下InterpolatingTextWatcher
     * {@code
     *  new InterpolatingTextWatcher(phoneNumInput, new int[]{3, 4}, new String[]{"-"});
     * }
     * 并且需设置{@link InputFilter.LengthFilter}以限定输入文本长度，但是注意，真正的文本长度上限并非11，还得加上插值。
     * 此时用户便可以使用该接口获取加上插值后的长度，如下：
     * {@code
     *   int maxLen = getMaxTextLength(11); // maxLen=13
     *   phoneNumInput.setFilters(new InputFilter[]{
     *      new InputFilter.LengthFilter(maxLen)
     *   });
     * }
     * */
    public int getMaxTextLength(int maxInputTextLen){
        int maxTextLen = maxInputTextLen;
        for (int i=0, cursor=spans[0];
             cursor < maxInputTextLen;
             ++i, cursor += i<spans.length ? spans[i] : spans[spans.length-1]){
            String separator = i<separators.length ? separators[i] : separators[separators.length-1];
            maxTextLen += separator.length();
            KLog.p("maxInputTextLen=%s, cursor=%s, separator=%s, maxTextLen=%s", maxInputTextLen, cursor, separator, maxTextLen);
        }
        return maxTextLen;
    }


}
