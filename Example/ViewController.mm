#import "ViewController.h"
#import <Algorithms/AlgoEngine.h>

@interface ViewController ()

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
    algolib::AlgoEngine engine;
    
    for (int i = 0; i < 100; i++) {
        double x = [self getRandomNumberBetween:-2 to:2];
        double y = [self getRandomNumberBetween:-2 to:2];
        double z = [self getRandomNumberBetween:-2 to:2];
        engine.process(x, y, z, [[NSDate date] timeIntervalSince1970]);
        auto state = engine.getState();
        NSLog(@"%f", state.activityLevel);
    }
}

-(double)getRandomNumberBetween:(int)from to:(int)to {
    return (double) from + rand() % (to-from+1);
}

@end
