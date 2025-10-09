package ex.org.project.submissionService.utils;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.LambdaException;

@Slf4j
public class LambdaUtils {


    public static void invokeFunction(LambdaClient awsLambda, String LambdaFunctionName) {
        try {

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(LambdaFunctionName)
                    .build();

           awsLambda.invoke(request);

        } catch (LambdaException e) {
            log.error(e.getMessage());
        }
    }
}
