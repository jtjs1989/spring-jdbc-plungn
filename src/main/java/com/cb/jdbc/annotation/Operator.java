package com.cb.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target( { ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Operator {
	public enum OperatorMark {EQUAL(" = "), GREATEROREQUAL(" >= ") ,GREATER(" > "), LESSOREQUAL(" <= "), LESS(" < "), LIKE(" like "), 
		NOTEQUAL(" <> "), IN(" in "), NOT_IN(" not in ");
		private String operator;
		OperatorMark(String operator){
			this.operator = operator;
		}
		public String getOpetator(){
			return this.operator;
		}
	};
	OperatorMark value() default OperatorMark.EQUAL;
}
