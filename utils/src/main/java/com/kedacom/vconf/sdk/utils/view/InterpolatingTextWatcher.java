package com.kedacom.vconf.sdk.utils.view;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.log.KLog;


/**
 * 插值文本监听器。
 * 该TextWatcher根据指定的span插入指定的separator。
 * */
public class InterpolatingTextWatcher implements TextWatcher {
    // 原始的文本内容（剔除插值）
    private StringBuilder rawText = new StringBuilder();
    // 删掉的内容
    private StringBuilder deletedText = new StringBuilder();
    // 内容变化的起点（剔除分隔符以后的）
    private int rawStart;
    // 添加插值后的内容
    private StringBuilder finalText = new StringBuilder();

    private EditText editText;
    private int[] spans;
    private String[] separators;
    private int begin;
    private int end;

    public static final int TILL_END = Integer.MAX_VALUE;

    /**
     * @param editText 监听的EditText
     * @param spans 插入的跨度
     * @param separators 插入的字符串
     * @param begin 计算的起始位置。若小于0则取值0，0表示第一个字符的位置。
     * @param end 计算的结束位置。若大于text长度则取text长度值；取值{@link #TILL_END}表示到text末尾。
     *            NOTE：计算范围为左闭右开区间即[begin,end)。如对于“1234567”，若begin=1, end=4，则计算的区间为"234"
     *
     * 举例：若editText输入内容“1234567”，span为{1}，separator为{"-"}，begin=0, end=7, 则最终展示在EditText上的内容为“1-2-3-4-5-6-7”。
     *      若editText输入内容“1234567”，span为{1,2}，separator为{"-"}，begin=0, end=7, 则最终展示在EditText上的内容为“1-23-45-67”。
     *      若editText输入内容“1234567”，span为{1}，separator为{"-",":"}，begin=0, end=7, 则最终展示在EditText上的内容为“1-2:3:4:5:6:7”。
     *      若editText输入内容“1234567”，span为{1,2}，separator为{"-",":"}，begin=0, end=7, 则最终展示在EditText上的内容为“1-23:45:67”。
     *      若editText输入内容“1234567”，span为{1,2}，separator为{"-",":"}，begin=0, end=4, 则最终展示在EditText上的内容为“1-23:4567”。
     *      若editText输入内容“1234567”，span为{1,2}，separator为{"-",":"}，begin=1, end=4, 则最终展示在EditText上的内容为“12-34567”。
     * */
    public InterpolatingTextWatcher(@NonNull EditText editText, @NonNull int[] spans, @NonNull String[] separators, int begin, int end) {
        this.editText = editText;
        this.spans = new int[spans.length];
        this.separators = new String[separators.length];
        System.arraycopy(spans, 0, this.spans, 0, spans.length);
        System.arraycopy(separators, 0, this.separators, 0, separators.length);
        this.begin = Math.max(begin, 0);
        this.end = end;
        for (int i=0; i<spans.length; ++i){
            KLog.p("spans[%s]=%s", i, spans[i]);
        }
        for (int i=0; i<separators.length; ++i){
            KLog.p("separators[%s]=%s", i, separators[i]);
        }
        KLog.p("begin=%s, end=%s", begin, end);
    }

    /**
     * @see #InterpolatingTextWatcher(EditText, int[], String[], int, int)
     * */
    public InterpolatingTextWatcher(@NonNull EditText editText, @NonNull int[] spans, @NonNull String[] separators) {
        this(editText, spans, separators, 0, TILL_END);
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

        // 剔除插入的间隔符，拿到原始字符串
        // NOTE：text内容中也有可能包含间隔符，不能剔除那部分，所以我们不能通过内容比对剔除间隔符，而要通过位置。
        rawStart = start;
        for (int i=0, span=begin+spans[i];
             span<Math.min(end, rawText.length());
             ++i, span += i<spans.length ? spans[i] : spans[spans.length-1]){
            KLog.p("spans[%s]=%s, rawText=%s, rawStart=%s", i, span, rawText, rawStart);
            String separator = i<separators.length ? separators[i] : separators[separators.length-1];
            int separatorLen = separator.length();
            rawText.delete(span, span+separatorLen);
            if (span< deletedText.length()) {
                deletedText.delete(span, span+separatorLen);
            }
            if (span<=rawStart){
                rawStart -= separatorLen;
            }
        }

        deletedText.delete(0, rawStart);
        KLog.p("rawText=%s, rawStart=%s, deletedText=%s", rawText, rawStart, deletedText);
        if (deletedText.length() != 0) {
            rawText.delete(rawStart, rawStart+ deletedText.length());
        }
        KLog.p("rawDeletedPart=%s, rawStart=%s, rawText=%s", deletedText, rawStart, rawText);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        KLog.p("s=%s, start=%s, before=%s, count=%s, rawText=%s", s, start, before, count, rawText);
        CharSequence addedText = s.subSequence(start, start+count);
        rawText.insert(rawStart, addedText);
//                String trimedStr = rawStr.toString().replace(" ", "");

        KLog.p("rawText=%s, addedText=%s", rawText, addedText);
    }

    @Override
    public void afterTextChanged(Editable s) {
        KLog.p("s=%s", s);
        if (begin>=end){
            KLog.p(KLog.ERROR, "begin(%s)>=end(%s)", begin, end);
            return;
        }
        if (begin>=rawText.length()){
            KLog.p(KLog.ERROR, "begin(%s)>=rawText.length(%s)", begin, rawText.length());
            return;
        }
        finalText.delete(0, finalText.length());
        finalText.append(rawText);
        // 对原始text进行插值处理生成最终text。
        // NOTE: xml中EditText的inputType属性可能不包括separator，如inputType=number，separator=" "，
        // 但是代码中setText不属于input，故不受inputType属性的影响。
        // 代码中setFilters会影响所有输入的内容包括键盘输入、复制粘贴、代码中setText
        int stop = Math.min(end, rawText.length());
        KLog.p("begin=%s, stop=%s, spans[0]=%s, rawText=%s", begin, stop, spans[0], rawText);
        for (int i=0, span=begin+spans[i];
             span<Math.min(stop, finalText.length());
             ++i, span += i<spans.length ? spans[i] : spans[spans.length-1]){
            String sep = i<separators.length ? separators[i] : separators[separators.length-1];
            int sepLen = sep.length();
            finalText.insert(span, sep);
            span += sepLen;
            stop += sepLen;
            KLog.p("span=%s, stop=%s, sep=%s, finalText=%s", span, stop, sep, finalText);
        }

        KLog.p("rawText=%s, finalText=%s", rawText, finalText);
        // setText的行为是先触发“beforeTextChanged->onTextChanged->afterTextChanged”然后才继续往下执行，这非我们期望，故暂时删除listener
        editText.removeTextChangedListener(this);
        editText.setText(finalText); // NOTE: 受filter影响，此处设置的finalText不一定等同于editText.getText()
        editText.addTextChangedListener(this);

        editText.setSelection(editText.getText().toString().length()); // 更新光标位置到最末尾。由于我们修改了Text内容原来的光标位置也需相应更新
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
     * 获取EditText的最大长度。该长度可用于{@link android.text.InputFilter.LengthFilter}
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
