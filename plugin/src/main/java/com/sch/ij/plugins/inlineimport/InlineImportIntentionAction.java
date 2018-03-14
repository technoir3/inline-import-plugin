package com.sch.ij.plugins.inlineimport;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.SourceJavaCodeReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InlineImportIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {
    @NotNull
    @Override
    public String getText() {
        return "Inline import";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        final PsiFile psiFile = PsiTreeUtil.getParentOfType(psiElement, PsiFile.class);
        final PsiImportStatement importStatement = PsiTreeUtil.getParentOfType(psiElement, PsiImportStatement.class);
        if (psiFile != null && importStatement != null) {
            final List<PsiJavaCodeReferenceElement> references = collectReferencesThrough(psiFile, importStatement);
            replaceAllAndDeleteImport(references, importStatement);
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        return PsiTreeUtil.getParentOfType(psiElement, PsiImportStatement.class) != null;
    }

    private List<PsiJavaCodeReferenceElement> collectReferencesThrough(PsiFile file, PsiImportStatement importStatement) {
        final List<PsiJavaCodeReferenceElement> expressionToExpand = new ArrayList<>();
        file.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitReferenceElement(PsiJavaCodeReferenceElement expression) {
                final PsiElement resolveScope = expression.advancedResolve(true).getCurrentFileResolveScope();
                if (resolveScope == importStatement) {
                    expressionToExpand.add(expression);
                }
                super.visitElement(expression);
            }
        });
        return expressionToExpand;
    }

    private void replaceAllAndDeleteImport(List<PsiJavaCodeReferenceElement> expressionToExpand, PsiImportStatement importStatement) {
        expressionToExpand.sort((o1, o2) -> o2.getTextOffset() - o1.getTextOffset());

        for (PsiJavaCodeReferenceElement expression : expressionToExpand) {
            expand(expression, importStatement);
        }

        importStatement.delete();
    }

    private void expand(PsiJavaCodeReferenceElement refExpr, PsiImportStatement importStatement) {
        final PsiClass targetClass = (PsiClass) importStatement.resolve();
        if (targetClass != null && refExpr instanceof SourceJavaCodeReference) {
            ((SourceJavaCodeReference) refExpr).fullyQualify(targetClass);
        }
    }
}
